package tictactoe4k

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
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
            "/game/{gameId}" bind GET to ::getGame,
            "/game/{gameId}/moves" bind POST to ::makeMove
        ).withFilter(CatchAllExceptions())

    override fun invoke(request: Request) = httpHandler(request)

    private fun newGame(): Response {
        val gameId = gameRepository.add(Game())
        return Response(OK).body(gameId)
    }

    private fun getGame(request: Request): Response {
        val gameId = parseGameId(request) ?: return Response(BAD_REQUEST).body("game id is required")
        val game = gameRepository.find(gameId).onFailure { return it.reason.toResponse() }
        return Response(OK).body(game.toJson())
    }

    private fun makeMove(request: Request): Response {
        val gameId = parseGameId(request) ?: return Response(BAD_REQUEST).body("game id is required")
        val x = parseX(request) ?: return Response(BAD_REQUEST).body("x and y are required")
        val y = parseY(request) ?: return Response(BAD_REQUEST).body("x and y are required")

        return when (val result = makeMove(gameId, x, y)) {
            is Failure<GameError> -> result.reason.toResponse()
            is Success<*> -> Response(OK)
        }
    }

    private fun makeMove(gameId: String, x: Int, y: Int): Result4k<Game, GameError> {
        val game = gameRepository.find(gameId).onFailure { return it }
        val updatedGame = game.makeMove(x, y).onFailure { return it }
        return gameRepository.update(gameId, updatedGame)
    }

    private fun GameError.toResponse(): Response =
        when (val reason = this) {
            is OutOfRangeMove -> Response(CONFLICT).body("Move is out of range x=${reason.x}, y=${reason.y}")
            is DuplicateMove -> Response(CONFLICT).body("Duplicate move x=${reason.x}, y=${reason.y}")
            is MoveAfterGameOver -> Response(CONFLICT).body("Game is over")
            is GameNotFound -> Response(BAD_REQUEST).body("Game not found id='${reason.gameId}'")
        }

    private fun parseGameId(request: Request): String? =
        request.path("gameId")

    private fun parseX(request: Request): Int? =
        try {
            request.query("x")?.toInt()
        } catch (e: NumberFormatException) {
            null
        }

    private fun parseY(request: Request): Int? =
        try {
            request.query("y")?.toInt()
        } catch (e: NumberFormatException) {
            null
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

fun Game.toJson(): String =
    Jackson.mapper.writeValueAsString(this)

fun String.parseGameJson(): Game =
    Jackson.mapper.readValue(this, Game::class.java)

fun Response.parseGameJson(): Game =
    bodyString().parseGameJson()
