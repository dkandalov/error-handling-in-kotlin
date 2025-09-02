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
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import tictactoe4k.game.*
import java.util.concurrent.ConcurrentHashMap

class WebApp(val gameStore: GameStore) {
    private val htmlRenderer = HandlebarsTemplates().HotReload("src/main/resources")
    val httpHandler =
        routes(
            "/" bind GET to { newGame() },
            "/game/{gameId}" bind GET to ::findGame,
            "/game/{gameId}/move" bind GET to ::makeMove,
            "/static" bind static(ResourceLoader.Classpath("public"))
        ).withFilter(HandleUnexpectedExceptions(htmlRenderer)).withFilter(UserIdCookieFilter(gameStore))

    private val subscribersByGame = ConcurrentHashMap<GameId, MutableSet<Sse>>()
    val sseHandler = sse("/game/events" bind sse { subscribeToGameEvents(it) })

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
        val userId = UserId(request.cookie("userid")!!.value)

        gameStore.makeMove(gameId, x, y, userId)
        broadcastUpdate(gameId)

        return Response(SEE_OTHER).header("Location", "/game/$gameId")
    }

    private fun subscribeToGameEvents(sse: Sse) {
        val gameId = sse.connectRequest.parseGameId()
        val subscribers = subscribersByGame.getOrPut(gameId) { ConcurrentHashMap.newKeySet() }
        subscribers.add(sse)
        sse.onClose { subscribers.remove(sse) }
        sse.send(SseMessage.Event("connected", gameId.value))
    }

    private fun broadcastUpdate(gameId: GameId) {
        subscribersByGame[gameId]?.forEach { client ->
            try {
                client.send(SseMessage.Event("update", gameId.value))
            } catch (_: Exception) {
                subscribersByGame[gameId]?.remove(client)
            }
        }
    }
}

private fun Request.parseGameId() =
    (query("gameId") ?: path("gameId"))!!.let(::GameId)

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
        } catch (e: WrongPlayerMove) {
            val gameId = request.parseGameId()
            Response(SEE_OTHER).header("Location", "/game/$gameId")
        } catch (e: Exception) {
            e.printStackTrace()
            Response(OK).html(htmlRenderer(ErrorView(message = "Something went wrong 😭")))
        }
    }
}

private class UserIdCookieFilter(private val gameStore: GameStore) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        val userId = request.cookie("userid")?.let { UserId(it.value) }
        if (userId != null) next(request)
        else {
            val newUserId = gameStore.newUserId()
            val response = next(request.cookie("userid", newUserId.value))
            response.cookie(Cookie("userid", newUserId.value, path = "/", httpOnly = true))
        }
    }
}