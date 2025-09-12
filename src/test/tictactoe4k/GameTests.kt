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
            Game().makeMove_new(Move(0, 1, X)).orFail()
                .makeMove_new(Move(2, 0, O)).orThrow()
                .makeMove_new(Move(2, 1, X)).orThrow()

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
            .makeMove_new(Move(0, 0, X)).orThrow()
            .makeMove_new(Move(1, 1, O)).orThrow()

        assertFailsWith<DuplicateMove> { game.makeMove_new(Move(0, 0, X)).orThrow() }
    }

    @Test fun `can't make moves outside of the board`() {
        assertFailsWith<OutOfRangeMove> {
            Game().makeMove_new(Move(-1, 0, X)).orThrow()
        }
        assertFailsWith<OutOfRangeMove> {
            Game().makeMove_new(Move(0, 3, X)).orThrow()
        }
    }

    @Test fun `can't make moves when the game is over`() {
        assertFailsWith<MoveAfterGameOver> {
            Game().makeMoves(playerXWinningMoves).makeMove_new(Move(2, 2, X)).orThrow()
        }
    }

    @Test fun `can't make moves with wrong player`() {
        assertFailsWith<WrongPlayerMove> {
            Game().makeMove_new(Move(0, 0, O)).orThrow()
        }
        assertFailsWith<WrongPlayerMove> {
            Game().makeMove_new(Move(0, 0, X)).orThrow().makeMove_new(Move(0, 1, X)).orThrow()
        }
    }

    private fun Game.makeMoves(moves: List<Move>) =
        moves.fold(this) { game, move -> game.makeMove_new(move).orThrow() }
}

fun Result<Game, WrongPlayerMove>.orFail() =
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