package tictactoe4k

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import tictactoe4k.game.*
import kotlin.test.assertFailsWith

class GameTests {
    @Test fun `players take turns on each move`() {
        val updatedGame =
            Game().makeMove(0, 1)
                .makeMove(2, 0)
                .makeMove(2, 1)

        expectThat(updatedGame.moves).isEqualTo(
            listOf(
                Move(0, 1, Player.X),
                Move(2, 0, Player.O),
                Move(2, 1, Player.X)
            )
        )
        expectThat(updatedGame.isOver).isFalse()
    }

    @Test fun `player X wins`() {
        val game = Game().makeMoves(playerXWinningMoves)
        expectThat(game.winner).isEqualTo(Player.X)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `player O wins`() {
        val game = Game().makeMoves(playerOWinningMoves)
        expectThat(game.winner).isEqualTo(Player.O)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `game ends in a draw`() {
        val game = Game().makeMoves(gameEndsInDrawMoves)
        expectThat(game.winner).isEqualTo(null)
        expectThat(game.isOver).isTrue()
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make the same move twice`() {
        val game = Game()
            .makeMove(0, 0)
            .makeMove(1, 1)

        assertFailsWith<DuplicateMove> { game.makeMove(0, 0) }
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make moves outside of the board`() {
        assertFailsWith<OutOfRangeMove> { Game().makeMove(-1, 0) }
        assertFailsWith<OutOfRangeMove> { Game().makeMove(0, 3) }
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make moves when the game is over`() {
        assertFailsWith<MoveAfterGameOver> { Game().makeMoves(playerXWinningMoves).makeMove(2, 2) }
    }

    private fun Game.makeMoves(moves: List<Pair<Int, Int>>) =
        moves.fold(this) { game, (x, y) -> game.makeMove(x, y) }
}

val playerXWinningMoves = listOf(
    Pair(0, 0), Pair(1, 0),
    Pair(0, 1), Pair(1, 1),
    Pair(0, 2)
)

val playerOWinningMoves = listOf(
    Pair(0, 1), Pair(0, 0),
    Pair(0, 2), Pair(1, 1),
    Pair(1, 0), Pair(2, 2)
)

val gameEndsInDrawMoves = listOf(
    Pair(1, 1), Pair(0, 0),
    Pair(0, 1), Pair(0, 2),
    Pair(1, 0), Pair(1, 2),
    Pair(2, 0), Pair(2, 1),
    Pair(2, 2)
)
