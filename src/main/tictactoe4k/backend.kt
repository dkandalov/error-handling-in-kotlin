package tictactoe4k

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.ApacheServer
import org.http4k.server.asServer

fun main() {
    Backend(GameRepository())
        .asServer(ApacheServer(port = 1234))
        .start()
    println("Started backend on http://localhost:1234")
}

class Backend(private val gameRepository: GameRepository) : HttpHandler {
    private val httpHandler =
        routes(
            "/game" bind POST to { newGame() },
            "/game/{gameId}" bind GET to { request -> getGame(request) },
            "/game/{gameId}/moves" bind POST to { request -> makeMove(request) }
        ).withFilter(CatchAllExceptions())

    override fun invoke(request: Request) = httpHandler(request)

    private fun newGame(): Response {
        val gameId = gameRepository.add(Game())
        return Response(OK).body(gameId)
    }

    private fun getGame(request: Request): Response {
        return try {

            val gameId = parseGameId(request)
            val game = gameRepository.find(gameId)
            Response(OK).body(game.toJson())

        } catch (e: FailedToParseRequest) {
            Response(BAD_REQUEST).body(e.message)
        } catch (e: GameNotFound) {
            Response(BAD_REQUEST).body("Game not found id='${e.gameId}'")
        }
    }

    private fun makeMove(request: Request): Response {
        return try {

            val gameId = parseGameId(request)
            val x = parseX(request)
            val y = parseY(request)

            val game = gameRepository.find(gameId)
            val updatedGame = game.makeMove(x, y)
            gameRepository.update(gameId, updatedGame)

            Response(OK)

        } catch (e: FailedToParseRequest) {
            Response(BAD_REQUEST).body(e.message)
        } catch (e: GameNotFound) {
            Response(BAD_REQUEST).body("Game not found id='${e.gameId}'")
        } catch (e: OutOfRangeMove) {
            Response(CONFLICT).body("Move is out of range x=${e.x}, y=${e.y}")
        } catch (e: DuplicateMove) {
            Response(CONFLICT).body("Duplicate move x=${e.x}, y=${e.y}")
        } catch (e: MoveAfterGameOver) {
            Response(CONFLICT).body("Game is over")
        }
    }

    private fun parseGameId(request: Request): String {
        val gameId = request.path("gameId")
        if (gameId == null) throw FailedToParseRequest("game id is required")
        else return gameId
    }

    private fun parseX(request: Request): Int {
        try {
            val x = request.query("x")
            if (x == null) throw FailedToParseRequest("x and y are required")
            else return x.toInt()
        } catch (e: NumberFormatException) {
            throw FailedToParseRequest(e.message ?: "")
        }
    }

    private fun parseY(request: Request): Int {
        try {
            val y = request.query("y")
            if (y == null) throw FailedToParseRequest("x and y are required")
            else return y.toInt()
        } catch (e: NumberFormatException) {
            throw FailedToParseRequest(e.message ?: "")
        }
    }
}

class FailedToParseRequest(override val message: String) : Exception(message)

private class CatchAllExceptions : Filter {
    override fun invoke(httpHandler: HttpHandler): HttpHandler = { request ->
        try {
            httpHandler(request)
        } catch (e: Exception) {
            Response(INTERNAL_SERVER_ERROR).body(e.message ?: "")
        }
    }
}

fun Game.toJson(): String {
    return Jackson.mapper.writeValueAsString(this)
}

fun String.parseGameJson(): Game {
    return Jackson.mapper.readValue(this, Game::class.java)
}

fun Response.parseGameJson(): Game {
    return Jackson.mapper.readValue(bodyString(), Game::class.java)
}
