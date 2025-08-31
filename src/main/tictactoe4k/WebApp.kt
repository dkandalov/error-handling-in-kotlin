package tictactoe4k

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.lens.html
import org.http4k.routing.*
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import tictactoe4k.game.Game
import tictactoe4k.game.GameId
import tictactoe4k.game.GameStore

class WebApp(val gameStore: GameStore) : HttpHandler {
    private val htmlRenderer = HandlebarsTemplates().HotReload("src/main/resources")
    private val httpHandler =
        routes(
            "/" bind GET to { newGame() },
            "/game/{gameId}" bind GET to ::findGame,
            "/game/{gameId}/move" bind GET to ::makeMove,
            "/static" bind static(ResourceLoader.Classpath("public"))
        ).withFilter(HandleUnexpectedExceptions(htmlRenderer)).withFilter(UserIdCookieFilter(gameStore))

    override fun invoke(request: Request) =
        httpHandler(request)

    private fun newGame(): Response {
        val gameId = gameStore.newGame()
        return Response(SEE_OTHER).header("Location", "/game/$gameId")
    }

    private fun findGame(request: Request): Response {
        val gameId = request.parseGameId()
        val game = gameStore.findGame(gameId)
        return Response(OK).html(htmlRenderer(GameView(game, gameId)))
    }

    private fun makeMove(request: Request): Response {
        val gameId = request.parseGameId()
        val x = request.query("x")!!.toInt()
        val y = request.query("y")!!.toInt()

        gameStore.makeMove(gameId, x, y)
        return Response(SEE_OTHER).header("Location", "/game/$gameId")
    }

    private fun Request.parseGameId() =
        path("gameId")!!.let(::GameId)
}

private class GameView(
    val gameId: String,
    val rows: List<List<CellView>>,
    val winner: String?,
    val isOver: Boolean,
) : ViewModel

private fun GameView(game: Game, gameId: GameId) =
    GameView(
        gameId = gameId.value,
        rows = (0..2).map { x ->
            (0..2).map { y ->
                val player = game.moves.find { it.x == x && it.y == y }?.player?.name
                CellView(x, y, player)
            }
        },
        winner = game.winner?.name,
        isOver = game.isOver
    )

private class CellView(val x: Int, val y: Int, val player: String?)

private class ErrorView(val message: String, val gameId: String? = null) : ViewModel

private class HandleUnexpectedExceptions(private val htmlRenderer: TemplateRenderer) : Filter {
    override fun invoke(handler: HttpHandler): HttpHandler = { request ->
        try {
            handler(request)
        } catch (e: Exception) {
            Response(OK).html(htmlRenderer(ErrorView(message = "Something went wrong 😭")))
        }
    }
}

private class UserIdCookieFilter(private val gameStore: GameStore) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        val response = next(request)
        if (request.cookie("userid") != null) response
        else response.cookie(Cookie("userid", gameStore.newUserId(), path = "/", httpOnly = true))
    }
}