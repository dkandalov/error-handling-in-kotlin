import com.intellij.codeInsight.inline.completion.InlineCompletionEvent
import com.intellij.codeInsight.inline.completion.InlineCompletionProvider
import com.intellij.codeInsight.inline.completion.InlineCompletionProviderID
import com.intellij.codeInsight.inline.completion.InlineCompletionRequest
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.folding.LanguageFolding
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

InlineCompletionProvider.EP_NAME.point.registerExtension(DemoInlineProvider(), LoadingOrder.FIRST, pluginDisposable)

class DemoInlineProvider : InlineCompletionProvider {
    override val id = InlineCompletionProviderID("foo")

    override fun isEnabled(event: InlineCompletionEvent): Boolean {
        val text = event.toRequest()?.document?.immutableCharSequence ?: return false
        return text.contains("class Composite") && !text.contains("private val closables: MutableList")
    }

    override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
        return InlineCompletionSingleSuggestion.build {
            emit(
                InlineCompletionTextElement(
                    text = """
                        Closable(private val closables: MutableList<AutoCloseable> = CopyOnWriteArrayList()) : AutoCloseable {
                            fun add(closable: AutoCloseable) =
                                closables.add(closable)
                            
                            override fun close() =
                                closables.reversed().forEach { it.close() }
                            
                            fun closeOnShutdown() = apply {
                                Runtime.getRuntime().addShutdownHook(Thread(this::close))
                            }
                        }
                    """.trimIndent(),
                    TextAttributes()
                )
            )
        }
    }
}

val pluginDescriptor = DefaultPluginDescriptor(PluginId.getId("LivePlugin"), OnFailureReturnItFoldingBuilder::class.java.classLoader)

listOf("OnFailureReturnItFoldingBuilder", "OnFailureReturnLambdaFoldingBuilder", "AsSuccessFailureFoldingBuilder", "OrThrowFoldingBuilder")
    .map { LanguageExtensionPoint<FoldingBuilder>("kotlin", "Plugin\$$it", pluginDescriptor) }
    .forEach { LanguageFolding.EP_NAME.point.registerExtension(it, pluginDisposable) }


abstract class CommonFoldingBuilder : FoldingBuilderEx() {
    abstract val pattern: Regex
    abstract val placeholderText: String

    open fun getRange(match: MatchResult): TextRange =
        TextRange(match.range.first, match.range.last + 1)

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean) =
        pattern.findAll(document.charsSequence)
            .map { match -> FoldingDescriptor(root.node, getRange(match)) }
            .toList().toTypedArray()

    override fun getPlaceholderText(node: ASTNode) = placeholderText
    override fun isCollapsedByDefault(node: ASTNode) = true
}

class OnFailureReturnItFoldingBuilder : CommonFoldingBuilder() {
    override val pattern = Regex("\\.onFailure \\{ return it \\}")
    override val placeholderText = "?"
}
class OnFailureReturnLambdaFoldingBuilder : CommonFoldingBuilder() {
    override val pattern = Regex("\\.onFailure \\{.*\\}")
    override val placeholderText = "?:"
    override fun getRange(match: MatchResult) =
        TextRange(match.range.first, match.range.first + 10)
}
class AsSuccessFailureFoldingBuilder : CommonFoldingBuilder() {
    override val pattern = Regex("\\.asSuccess\\(\\)|\\.asFailure\\(\\)")
    override val placeholderText = " "
}
class OrThrowFoldingBuilder : CommonFoldingBuilder() {
    override val pattern = Regex("\\.orThrow\\(\\)")
    override val placeholderText = "!"
}