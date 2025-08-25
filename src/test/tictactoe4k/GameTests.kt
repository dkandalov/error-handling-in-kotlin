package tictactoe4k

import dev.forkhandles.result4k.asFailure
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class GameTests {
    @Test fun `players take turns on each move`() {
        val updatedGame =
            Game().makeMove(0, 1).expectSuccess()
                .makeMove(2, 0).expectSuccess()
                .makeMove(2, 1).expectSuccess()

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
        val game = gameWonByPlayerX()
        expectThat(game.winner).isEqualTo(Player.X)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `player O wins`() {
        val game = gameWonByPlayerO()
        expectThat(game.winner).isEqualTo(Player.O)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `game ends in a draw`() {
        val game = gameEndsInDraw()
        expectThat(game.winner).isEqualTo(null)
        expectThat(game.isOver).isTrue()
    }

    @Test fun `can't make the same move twice`() {
        val game = Game()
            .makeMove(0, 0).expectSuccess()
            .makeMove(1, 1).expectSuccess()

        expectThat(game.makeMove(0, 0)).isEqualTo(DuplicateMove(0, 0).asFailure())
    }

    @Test fun `can't make moves outside of the board`() {
        expectThat(Game().makeMove(-1, 0)).isEqualTo(OutOfRangeMove(-1, 0).asFailure())
        expectThat(Game().makeMove(0, 3)).isEqualTo(OutOfRangeMove(0, 3).asFailure())
    }

    @Test fun `can't make moves when the game is over`() {
        expectThat(gameWonByPlayerX().makeMove(2, 2))
            .isEqualTo(MoveAfterGameOver.asFailure())
    }
}

fun gameWonByPlayerX() =
    listOf(
        Pair(0, 0), Pair(1, 0),
        Pair(0, 1), Pair(1, 1),
        Pair(0, 2)
    ).fold(Game()) { game, (x, y) ->
        game.makeMove(x, y).expectSuccess()
    }

fun gameWonByPlayerO() =
    listOf(
        Pair(0, 1), Pair(0, 0),
        Pair(0, 2), Pair(1, 1),
        Pair(1, 0), Pair(2, 2)
    ).fold(Game()) { game, (x, y) ->
        game.makeMove(x, y).expectSuccess()
    }

fun gameEndsInDraw() =
    listOf(
        Pair(1, 1), Pair(0, 0),
        Pair(0, 1), Pair(0, 2),
        Pair(1, 0), Pair(1, 2),
        Pair(2, 0), Pair(2, 1),
        Pair(2, 2)
    ).fold(Game()) { game, (x, y) ->
        game.makeMove(x, y).expectSuccess()
    }