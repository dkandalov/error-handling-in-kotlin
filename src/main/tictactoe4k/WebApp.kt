package tictactoe4k

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.asResultOr
import dev.forkhandles.result4k.onFailure
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.html
import org.http4k.routing.*
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

class WebApp(val app: TicTacToeApp) : HttpHandler {
    private val htmlRenderer = HandlebarsTemplates().HotReload("src/main/resources")
    private val httpHandler =
        routes(
            "/" bind GET to { newGame() },
            "/game/{gameId}" bind GET to ::findGame,
            "/game/{gameId}/move" bind GET to ::makeMove,
            "/static" bind static(ResourceLoader.Classpath("public"))
        ).withFilter(HandleUnexpectedExceptions(htmlRenderer))

    override fun invoke(request: Request) =
        httpHandler(request)

    private fun newGame(): Response {
        val gameId = app.add(Game())
        return Response(SEE_OTHER).header("Location", "/game/$gameId")
    }

    private fun findGame(request: Request): Response {
        val gameId = request.parseGameId().onFailure { return it.toResponse() }
        return try {
            val game = app.findGame(gameId)
            game.toView(gameId).toResponse(htmlRenderer)
        } catch (e: Exception) {
            e.toResponse()
        }
    }

    private fun makeMove(request: Request): Response {
        val gameId = request.parseGameId().onFailure { return it.toResponse() }
        val x = request.parseX().onFailure { return it.toResponse(gameId) }
        val y = request.parseY().onFailure { return it.toResponse(gameId) }

        return try {
            app.makeMove(gameId, x, y)
            Response(SEE_OTHER).header("Location", "/game/$gameId")
        } catch (e: Exception) {
            e.toResponse(gameId)
        }
    }

    private fun Failure<ParsingError>.toResponse(gameId: GameId? = null) =
        ErrorView("Invalid or missing ${reason.message}", gameId?.value).toResponse(htmlRenderer)

    private fun Exception.toResponse(gameId: GameId? = null) =
        ErrorView(
            message = when (val it = this) {
                is MoveAfterGameOver -> "You can't move after the game is over"
                is OutOfRangeMove -> "The move was out of range x=${it.x}, y=${it.y}"
                is DuplicateMove -> "You can't move twice x=${it.x}, y=${it.y}"
                is GameNotFound -> "Game not found"
                else -> "Something went wrong 😭"
            },
            gameId = gameId?.value
        ).toResponse(htmlRenderer)

    private fun ViewModel.toResponse(htmlRenderer: TemplateRenderer) =
        Response(OK).html(htmlRenderer(this))

    private fun Request.parseX() =
        query("x")?.toIntOrNull().asResultOr { ParsingError("X") }

    private fun Request.parseY() =
        query("y")?.toIntOrNull().asResultOr { ParsingError("Y") }

    private fun Request.parseGameId() =
        path("gameId")?.let(::GameId).asResultOr { ParsingError("game id") }

    private data class ParsingError(val message: String)

    private fun Game.toView(gameId: GameId) =
        GameView(
            gameId = gameId.value,
            rows = (0..2).map { x ->
                (0..2).map { y ->
                    val player = moves.find { it.x == x && it.y == y }?.player?.name
                    CellView(x, y, player)
                }
            },
            winner = winner?.name,
            isOver = isOver
        )
}

private class GameView(
    val gameId: String,
    val rows: List<List<CellView>>,
    val winner: String?,
    val isOver: Boolean,
) : ViewModel

private class CellView(val x: Int, val y: Int, val player: String?)

private class ErrorView(val message: String, val gameId: String? = null) : ViewModel

private class HandleUnexpectedExceptions(private val htmlRenderer: TemplateRenderer) : Filter {
    override fun invoke(httpHandler: HttpHandler): HttpHandler = { request ->
        try {
            httpHandler(request)
        } catch (e: Exception) {
            Response(OK)
                .body(htmlRenderer(ErrorView("Something went wrong 😭")))
                .with(CONTENT_TYPE of TEXT_HTML)
        }
    }
}

private class FailedToParseRequest(override val message: String) : Exception(message)
