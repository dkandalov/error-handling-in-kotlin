package tictactoe4k

import strikt.api.expectThat
import strikt.assertions.*
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
        expectThat(updatedGame).isEqualTo(Game(
            moves = listOf(
                Move(0, 1, Player.X),
                Move(2, 0, Player.O),
                Move(2, 1, Player.X)
            )
        ))
        expectThat(updatedGame.isOver).isFalse()
    }

    @Test fun `player X wins`() {
        val game = backend.gameWonByPlayerX(id)
        expectThat(game.winner).isEqualTo(Player.X)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `player O wins`() {
        val game = backend.gameWonByPlayerO(id)
        expectThat(game.winner).isEqualTo(Player.O)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `game ends in a draw`() {
        val game = backend.gameEndsInDraw(id)
        expectThat(game.winner).isEqualTo(null)
        expectThat(game.isOver).isTrue()
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
