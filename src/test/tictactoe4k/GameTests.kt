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
            Game().makeMove(Move(0, 1, Player.X))
                .makeMove(Move(2, 0, Player.O))
                .makeMove(Move(2, 1, Player.X))

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
            .makeMove(Move(0, 0, Player.X))
            .makeMove(Move(1, 1, Player.O))

        assertFailsWith<DuplicateMove> { game.makeMove(Move(0, 0, Player.X)) }
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make moves outside of the board`() {
        assertFailsWith<OutOfRangeMove> { Game().makeMove(Move(-1, 0, Player.X)) }
        assertFailsWith<OutOfRangeMove> { Game().makeMove(Move(0, 3, Player.X)) }
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make moves when the game is over`() {
        assertFailsWith<MoveAfterGameOver> { Game().makeMoves(playerXWinningMoves).makeMove(Move(2, 2, Player.X)) }
    }

    private fun Game.makeMoves(moves: List<Move>) =
        moves.fold(this, Game::makeMove)
}

val playerXWinningMoves = listOf(
    Move(0, 0, Player.X), Move(1, 0, Player.O),
    Move(0, 1, Player.X), Move(1, 1, Player.O),
    Move(0, 2, Player.X)
)

val playerOWinningMoves = listOf(
    Move(0, 1, Player.X), Move(0, 0, Player.O),
    Move(0, 2, Player.X), Move(1, 1, Player.O),
    Move(1, 0, Player.X), Move(2, 2, Player.O)
)

val gameEndsInDrawMoves = listOf(
    Move(1, 1, Player.X), Move(0, 0, Player.O),
    Move(0, 1, Player.X), Move(0, 2, Player.O),
    Move(1, 0, Player.X), Move(1, 2, Player.O),
    Move(2, 0, Player.X), Move(2, 1, Player.O),
    Move(2, 2, Player.X)
)