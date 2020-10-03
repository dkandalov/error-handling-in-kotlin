package tictactoe4k

import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.ApacheServer
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel

fun main() {
    val backendClient = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:1234")).then(OkHttp())
    Frontend(backendClient)
        .asServer(ApacheServer(port = 8080))
        .start()

    println("Started frontend on http://localhost:8080")
}

class Frontend(private val backend: HttpHandler) : HttpHandler {
    private val htmlRenderer = HandlebarsTemplates().HotReload("src/main/resources")

    private val httpHandler =
        routes(
            "/" bind GET to { newGame() },
            "/game/{gameId}" bind GET to { request -> getGame(request) },
            "/game/{gameId}/move" bind GET to { request -> makeMove(request) }
        ).withFilter(ShowErrorPageOnException { htmlRenderer(ErrorView) })

    override fun invoke(request: Request) = httpHandler(request)

    private fun newGame(): Response {
        val id = backend(Request(POST, "/game")).bodyString()
        return Response(SEE_OTHER).header("Location", "/game/$id")
    }

    private fun getGame(request: Request): Response {
        val gameId = parseGameId(request)
        val game = backend(Request(GET, "/game/$gameId")).bodyString().parseGameJson()
        return Response(OK)
            .body(htmlRenderer(game.toView(gameId)))
            .with(CONTENT_TYPE of TEXT_HTML)
    }

    private fun makeMove(request: Request): Response {
        val gameId = parseGameId(request)
        val x = request.query("x")
        val y = request.query("y")
        backend(Request(POST, "/game/$gameId/moves?x=$x&y=$y"))
        return Response(SEE_OTHER).header("Location", "/game/$gameId")
    }

    private fun parseGameId(request: Request): String {
        return request.path("gameId") ?: throw BadRequest("game id is required")
    }

    private fun Game.toView(gameId: String) = GameView(
        rows = (0..2).map { x ->
            (0..2).map { y ->
                val player = moves.find { it.x == x && it.y == y }?.player?.name
                CellView(x, y, gameId, player)
            }
        },
        winner = winner?.name,
        isOver = isOver
    )
}

private class GameView(
    val rows: List<List<CellView>>,
    val winner: String?,
    val isOver: Boolean
) : ViewModel

private class CellView(val x: Int, val y: Int, val gameId: String, val player: String?)

private object ErrorView : ViewModel

private class ShowErrorPageOnException(val errorPageHtml: (Exception) -> String) : Filter {
    override fun invoke(httpHandler: HttpHandler): HttpHandler = { request ->
        try {
            httpHandler(request)
        } catch (e: Exception) {
            Response(OK)
                .body(errorPageHtml(e))
                .with(CONTENT_TYPE of TEXT_HTML)
        }
    }
}
