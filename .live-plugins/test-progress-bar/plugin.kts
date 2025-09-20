import com.intellij.codeInsight.inline.completion.InlineCompletionEvent
import com.intellij.codeInsight.inline.completion.InlineCompletionProvider
import com.intellij.codeInsight.inline.completion.InlineCompletionProviderID
import com.intellij.codeInsight.inline.completion.InlineCompletionRequest
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.folding.LanguageFolding
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon.Position.above
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType
import com.intellij.psi.PsiElement
import com.intellij.ui.Gray
import com.intellij.ui.TransparentPanel
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBInsets
import liveplugin.currentEditor
import liveplugin.newDisposable
import liveplugin.registerParent
import liveplugin.runLaterOnEdt
import java.awt.Dimension
import java.awt.Point
import javax.swing.JProgressBar

TestsWatcher(project!!, pluginDisposable).start(
    listeners = listOf(TestsProgress(project!!, pluginDisposable))
)

class TestsWatcher(private val project: Project, private val disposable: Disposable) {

    fun start(listeners: List<Listener>) {
        var totalTestCount = 0
        var testsFinished = 0
        var testsFailed = 0
        val listener = object : SMTRunnerEventsAdapter() {
            override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
                totalTestCount = 0
                testsFinished = 0
                testsFailed = 0
                listeners.forEach { it.onTestsStarted() }
            }

            // Not called when running tests via Gradle
            override fun onSuiteTreeNodeAdded(testProxy: SMTestProxy?) {
                totalTestCount++
                listeners.forEach { it.onTestAdded(totalTestCount) }
            }

            override fun onTestStarted(test: SMTestProxy) {
                if (totalTestCount == 0) {
                    // This is a workaround for Gradle runner which doesn't seem to have a way to know the amount of tests
                    listeners.forEach { it.onTestAdded(test.parent.allTests.size) }
                }
            }

            override fun onTestFailed(test: SMTestProxy) {
                testsFailed++
                listeners.forEach { it.onTestFailed() }
            }

            override fun onTestFinished(test: SMTestProxy) {
                testsFinished++
                listeners.forEach { it.onTestFinished(testsFinished) }
            }

            override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
                if (totalTestCount != 0 && testsFinished != totalTestCount) {
                    listeners.forEach { it.onTestsStopped() }
                } else if (testsFailed > 0) {
                    listeners.forEach { it.onTestsFailed() }
                } else {
                    listeners.forEach { it.onTestsSucceeded() }
                }
            }
        }
        project.messageBus.connect(disposable)
            .subscribe(SMTRunnerEventsListener.TEST_STATUS, listener)
    }

    interface Listener {
        fun onTestsStarted() {}
        fun onTestAdded(count: Int) {}
        fun onTestFinished(count: Int) {}
        fun onTestFailed() {}
        fun onTestsSucceeded() {}
        fun onTestsFailed() {}
        fun onTestsStopped() {}
    }
}

class TestsProgress(val project: Project, private val parentDisposable: Disposable) : TestsWatcher.Listener {
    private lateinit var progressPanel: TestsProgressPanel

    override fun onTestsStarted() {
        progressPanel = TestsProgressPanel(project, parentDisposable)
        runLaterOnEdt { progressPanel.show() }
    }

    override fun onTestAdded(count: Int) {
        progressPanel.progressBar.maximum = count
    }

    override fun onTestFinished(count: Int) {
        progressPanel.progressBar.model.value = count
    }

    override fun onTestFailed() {
        progressPanel.progressBar.foreground = ColorProgressBar.RED
    }

    override fun onTestsSucceeded() = progressPanel.hide()
    override fun onTestsFailed() = progressPanel.hide()
    override fun onTestsStopped() = progressPanel.hide()

    private class TestsProgressPanel(private val project: Project, parentDisposable: Disposable) {
        private val disposable = newDisposable().registerParent(parentDisposable)
        val progressBar = JProgressBar(0, 100)

        fun show(): TestsProgressPanel {
            progressBar.apply {
                foreground = ColorProgressBar.GREEN
                preferredSize = Dimension(500, 20)
            }
            val panel = TransparentPanel(0.5f).apply {
                add(progressBar)
            }

            val balloon = JBPopupFactory.getInstance().createBalloonBuilder(panel)
                .setFadeoutTime(0)
                .setFillColor(Gray.TRANSPARENT)
                .setShowCallout(false)
                .setBorderColor(Gray.TRANSPARENT)
                .setBorderInsets(JBInsets(0, 0, 0, 0))
                .setAnimationCycle(0)
                .setCloseButtonEnabled(false)
                .setHideOnClickOutside(false)
                .setDisposable(disposable)
                .setHideOnFrameResize(false)
                .setHideOnKeyOutside(false)
                .setBlockClicksThroughBalloon(true)
                .setHideOnAction(false)
                .setShadow(false)
                .createBalloon()

            val editorComponent = project.currentEditor?.component ?: return this
            balloon.show(RelativePoint(editorComponent, Point(editorComponent.width / 2, editorComponent.height - 150)), above)

            project.messageBus.connect(disposable).subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
                override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerEventType) {
                    balloon.revalidate()
                }
            })

            return this
        }

        fun hide() {
            Disposer.dispose(disposable)
        }
    }
}
