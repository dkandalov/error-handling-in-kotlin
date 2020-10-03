package tictactoe4k

import datsok.shouldEqual
import org.http4k.core.Status
import org.junit.jupiter.api.Test

/*
 * Testing Game via backend so that it's easier to refactor code during the talk without having to change tests.
 */
class GameViaBackendTests {
    private val id = "gameId"
    private val repository = GameRepository(mutableMapOf(id to Game()))
    private val backend = Backend(repository)

    @Test fun `players take turns on each move`() {
        backend.makeMove(id, 0, 1)
        backend.makeMove(id, 2, 0)
        backend.makeMove(id, 2, 1)

        val updatedGame = backend.findGame(id).parseGameJson()
        updatedGame shouldEqual Game(
            moves = listOf(
                Move(0, 1, Player.X),
                Move(2, 0, Player.O),
                Move(2, 1, Player.X)
            )
        )
        updatedGame.isOver shouldEqual false
    }

    @Test fun `player X wins`() {
        val game = backend.gameWonByPlayerX(id)
        game.winner shouldEqual Player.X
        game.isOver shouldEqual true
    }

    @Test fun `player O wins`() {
        val game = backend.gameWonByPlayerO(id)
        game.winner shouldEqual Player.O
        game.isOver shouldEqual true
    }

    @Test fun `game ends in a draw`() {
        val game = backend.gameEndsInDraw(id)
        game.winner shouldEqual null
        game.isOver shouldEqual true
    }

    @Test fun `can't make the same move twice`() {
        backend.makeMove(id, 0, 0).expectOK()
        backend.makeMove(id, 1, 1).expectOK()
        backend.makeMove(id, 0, 0).expect(Status.CONFLICT, "Duplicate move x=0, y=0")
    }

    @Test fun `can't make moves outside of the board`() {
        backend.makeMove(id, -1, 0).expect(Status.CONFLICT, "Move is out of range x=-1, y=0")
        backend.makeMove(id, 0, 3).expect(Status.CONFLICT, "Move is out of range x=0, y=3")
    }

    @Test fun `can't make moves when the game is over`() {
        backend.gameWonByPlayerX(id)
        backend.makeMove(id, 2, 2).expect(Status.CONFLICT, "Game is over")
    }
}
