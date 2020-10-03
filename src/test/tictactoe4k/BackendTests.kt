package tictactoe4k

import datsok.shouldEqual
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import tictactoe4k.Player.O
import tictactoe4k.Player.X
import java.util.concurrent.atomic.AtomicInteger

class BackendTests {
    private val id = "gameId"
    private val gameRepository = GameRepository(
        gamesById = mutableMapOf(id to Game()),
        generateId = generateSequentialIds()
    )
    private val backend = Backend(gameRepository)

    @Test fun `create new game`() {
        val gameId = backend(Request(POST, "/game")).expectOK().bodyString()
        val response = backend(Request(GET, "/game/$gameId")).expectOK()
        response.parseGameJson() shouldEqual Game()
    }

    @Test fun `get game state`() {
        val response = backend(Request(GET, "/game/$id")).expectOK()
        response.parseGameJson() shouldEqual Game()
    }

    @Test fun `players take turns on each move`() {
        backend.makeMove(id, 0, 1).expectOK()
        backend.makeMove(id, 2, 0).expectOK()
        backend.makeMove(id, 2, 1).expectOK()

        val response = backend(Request(GET, "/game/$id")).expectOK()
        response.parseGameJson() shouldEqual Game(
            moves = listOf(
                Move(0, 1, X),
                Move(2, 0, O),
                Move(2, 1, X)
            )
        )
    }

    @Test fun `can't get state of non-existent game`() {
        backend(Request(GET, "/game")).expect(METHOD_NOT_ALLOWED)
        backend(Request(GET, "/game/wrong-id")).expect(BAD_REQUEST, "Game not found id='wrong-id'")
    }

    @Test fun `can't make move in non-existent game`() {
        backend.makeMove("wrong-id", 0, 0).expect(BAD_REQUEST, "Game not found id='wrong-id'")
    }

    @Test fun `missing arguments on move request`() {
        backend(Request(POST, "/game/$id/moves")).expect(BAD_REQUEST, "x and y are required")
        backend(Request(POST, "/game/$id/moves?x=0")).expect(BAD_REQUEST, "x and y are required")
        backend(Request(POST, "/game/$id/moves?y=0")).expect(BAD_REQUEST, "x and y are required")
    }

    @Test fun `invalid type of arguments on move request`() {
        backend(Request(POST, "/game/$id/moves?x=foo&y=bar")).expect(BAD_REQUEST)
        backend(Request(POST, "/game/$id/moves?x=123&y=bar")).expect(BAD_REQUEST)
    }

    @Test fun `convert game to and from json`() {
        val game = Game(moves = listOf(
            Move(0, 0, X),
            Move(1, 1, O),
            Move(2, 2, X)
        ))
        val gameJson =
            """{"moves":[{"x":0,"y":0,"player":"X"},{"x":1,"y":1,"player":"O"},{"x":2,"y":2,"player":"X"}],"winner":null,"isOver":false}"""

        game.toJson() shouldEqual gameJson
        gameJson.parseGameJson() shouldEqual game
    }

    @Test fun `convert json to game with invalid state`() {
        val game = """{"moves":[],"winner":null,"isOver":true}""".parseGameJson()
        game.isOver shouldEqual true
    }
}

fun Backend.addGame() =
    this(Request(POST, "/game")).expectOK()

fun Backend.findGame(id: String) =
    this(Request(GET, "/game/$id"))

fun Backend.makeMove(id: String, x: Int, y: Int) =
    this(Request(POST, "/game/$id/moves?x=$x&y=$y"))

fun Backend.gameWonByPlayerX(id: String): Game {
    makeMove(id, 0, 0); makeMove(id, 1, 0)
    makeMove(id, 0, 1); makeMove(id, 1, 1)
    makeMove(id, 0, 2)
    return findGame(id).expectOK().parseGameJson()
}

fun Backend.gameWonByPlayerO(id: String): Game {
    makeMove(id, 0, 1); makeMove(id, 0, 0)
    makeMove(id, 0, 2); makeMove(id, 1, 1)
    makeMove(id, 1, 0); makeMove(id, 2, 2)
    return findGame(id).parseGameJson()
}

fun Backend.gameEndsInDraw(id: String): Game {
    makeMove(id, 1, 1); makeMove(id, 0, 0)
    makeMove(id, 0, 1); makeMove(id, 0, 2)
    makeMove(id, 1, 0); makeMove(id, 1, 2)
    makeMove(id, 2, 0); makeMove(id, 2, 1)
    makeMove(id, 2, 2)
    return findGame(id).parseGameJson()
}

fun Response.expectOK(): Response {
    status shouldEqual OK
    return this
}

fun Response.expect(expectedStatus: Status, expectedBody: String? = null): Response {
    status shouldEqual expectedStatus
    if (expectedBody != null) {
        bodyString() shouldEqual expectedBody
    }
    return this
}

fun generateSequentialIds(): () -> String {
    val id = AtomicInteger()
    return { id.incrementAndGet().toString() }
}

