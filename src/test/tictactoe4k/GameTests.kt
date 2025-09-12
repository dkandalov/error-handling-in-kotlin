package tictactoe4k

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.orThrow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import tictactoe4k.game.*
import tictactoe4k.game.Player.O
import tictactoe4k.game.Player.X
import kotlin.test.assertFailsWith
import kotlin.test.fail

class GameTests {
    @Test fun `players take turns on each move`() {
        val updatedGame =
            Game().makeMove(Move(0, 1, X)).orFail()
                .makeMove(Move(2, 0, O)).orFail()
                .makeMove(Move(2, 1, X)).orFail()

        expectThat(updatedGame.moves).isEqualTo(
            listOf(
                Move(0, 1, X),
                Move(2, 0, O),
                Move(2, 1, X)
            )
        )
        expectThat(updatedGame.isOver).isFalse()
        expectThat(updatedGame.nextPlayer).isEqualTo(O)
    }

    @Test fun `player X wins`() {
        val game = Game().makeMoves(playerXWinningMoves)
        expectThat(game.isOver).isTrue()
        expectThat(game.winner).isEqualTo(X)
        expectThat(game.nextPlayer).isEqualTo(null)
    }

    @Test fun `player O wins`() {
        val game = Game().makeMoves(playerOWinningMoves)
        expectThat(game.isOver).isTrue()
        expectThat(game.winner).isEqualTo(O)
        expectThat(game.nextPlayer).isEqualTo(null)
    }

    @Test fun `game ends in a draw`() {
        val game = Game().makeMoves(gameEndsInDrawMoves)
        expectThat(game.isOver).isTrue()
        expectThat(game.winner).isEqualTo(null)
        expectThat(game.nextPlayer).isEqualTo(null)
    }

    @Test fun `can't make the same move twice`() {
        val game = Game()
            .makeMove(Move(0, 0, X)).orFail()
            .makeMove(Move(1, 1, O)).orFail()

        assertFailsWith<DuplicateMove> { game.makeMove(Move(0, 0, X)).orThrow() }
    }

    @Test fun `can't make moves outside of the board`() {
        assertFailsWith<OutOfRangeMove> {
            Game().makeMove(Move(-1, 0, X)).orThrow()
        }
        assertFailsWith<OutOfRangeMove> {
            Game().makeMove(Move(0, 3, X)).orThrow()
        }
    }

    @Test fun `can't make moves when the game is over`() {
        assertFailsWith<MoveAfterGameOver> {
            Game().makeMoves(playerXWinningMoves).makeMove(Move(2, 2, X)).orThrow()
        }
    }

    @Test fun `can't make moves with wrong player`() {
        assertFailsWith<WrongPlayerMove> {
            Game().makeMove(Move(0, 0, O)).orThrow()
        }
        assertFailsWith<WrongPlayerMove> {
            Game().makeMove(Move(0, 0, X)).orThrow().makeMove(Move(0, 1, X)).orThrow()
        }
    }

    private fun Game.makeMoves(moves: List<Move>) =
        moves.fold(this) { game, move -> game.makeMove(move).orThrow() }
}

fun Result<Game, GameException>.orFail() =
    orThrow { fail("Expected Successful but was $this") }

val playerXWinningMoves = listOf(
    Move(0, 0, X), Move(1, 0, O),
    Move(0, 1, X), Move(1, 1, O),
    Move(0, 2, X)
)

val playerOWinningMoves = listOf(
    Move(0, 1, X), Move(0, 0, O),
    Move(0, 2, X), Move(1, 1, O),
    Move(1, 0, X), Move(2, 2, O)
)

val gameEndsInDrawMoves = listOf(
    Move(1, 1, X), Move(0, 0, O),
    Move(0, 1, X), Move(0, 2, O),
    Move(1, 0, X), Move(1, 2, O),
    Move(2, 0, X), Move(2, 1, O),
    Move(2, 2, X)
)