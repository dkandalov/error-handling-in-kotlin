package tictactoe4k

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Path
import org.http4k.lens.html
import org.http4k.lens.string
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.routing.static
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseResponse
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.TemplateRenderer
import tictactoe4k.game.*
import java.util.concurrent.ConcurrentHashMap

class WebApp(val gameStore: GameStore) {
    private val htmlRenderer = HandlebarsTemplates().HotReload("src/main/resources")
    private val subscribersByGame = ConcurrentHashMap<GameId, MutableSet<Sse>>()

    val httpHandler =
        routes(
            "/" bind GET to ::newGame,
            "/game/{gameId}" bind GET to ::viewGame,
            "/game/{gameId}/move" bind GET to ::makeMove,
            "/static" bind static(Classpath("public"))
        ).withFilter(HandleUnexpectedExceptions(htmlRenderer))
            .withFilter(CatchLensFailure())
            .withFilter(UserIdCookieFilter(gameStore))

    val sseHandler =
        sse("/game/{gameId}/events" bind ::subscribeToGameEvents)

    private fun newGame(request: Request): Response {
        val gameId = gameStore.newGame()
        val oldGameId = request.query("gameId")?.let(::GameId)
        if (oldGameId != null) {
            subscribersByGame[oldGameId]?.broadcast(Event("update", gameId.value))
            subscribersByGame.remove(oldGameId)
        }

        return Response(SEE_OTHER).header("Location", "/game/$gameId")
    }

    private fun viewGame(request: Request): Response {
        val gameId = request.parseGameId()
        val game = gameStore.findGame(gameId)
        return Response(OK).html(htmlRenderer(GameView(game, gameId)))
    }

    private fun makeMove(request: Request): Response {
        val gameId = request.parseGameId()
        val x = request.parseX()
        val y = request.parseY()
        val userId = request.userId()!!

        return gameStore.makeMove_new(gameId, x, y, userId)
            .map {
                subscribersByGame[gameId]?.broadcast(Event("update", gameId.value))
                Response(SEE_OTHER).header("Location", "/game/$gameId")
            }
            .mapFailure {
                when (it) {
                    is WrongPlayerMove -> Response(SEE_OTHER).header("Location", "/game/$gameId")
                    is CannotAddNewPlayer,
                    is DuplicateMove,
                    is GameNotFound,
                    is MoveAfterGameOver,
                    is OutOfRangeMove,
                        -> throw it
                }
            }
            .get()
    }

    private fun subscribeToGameEvents(connectRequest: Request): SseResponse =
        try {
            val gameId = connectRequest.parseGameId()
            gameStore.findGame(gameId)
            SseResponse { sse ->
                val subscribers = subscribersByGame.getOrPut(gameId) { ConcurrentHashMap.newKeySet() }
                subscribers.add(sse)
                sse.onClose { subscribers.remove(sse) }
                sse.send(Event("connected", gameId.value))
            }
        } catch (e: GameNotFound) {
            SseResponse(status = NOT_FOUND, consumer = {})
        }

    private fun MutableSet<Sse>.broadcast(message: SseMessage) {
        forEach { sse ->
            try {
                sse.send(message)
            } catch (_: Exception) {
                remove(sse)
            }
        }
    }

    private fun Request.parseGameId() =
        gameIdLens.extract(this).let(::GameId)

    private val gameIdLens = Path.string().of("gameId")

    private fun Request.parseX() =
        query("x")!!.toInt()

    private fun Request.parseY() =
        query("y")!!.toInt()
}

private class HandleUnexpectedExceptions(private val htmlRenderer: TemplateRenderer) : Filter {
    override fun invoke(handler: HttpHandler): HttpHandler = { request ->
        try {
            handler(request)
        } catch (e: Exception) {
            e.printStackTrace()
            Response(OK).html(htmlRenderer(ErrorView(message = "Something went wrong 😭")))
        }
    }
}

private class UserIdCookieFilter(private val gameStore: GameStore) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        val userId = request.userId()
        if (userId != null) next(request)
        else {
            val newUserId = gameStore.newUserId()
            val response = next(request.cookie("userId", newUserId.value))
            response.cookie(Cookie("userId", newUserId.value, path = "/", httpOnly = true))
        }
    }
}

private fun Request.userId() =
    cookie("userId")?.value?.let(::UserId)
