package tictactoe4k

import dev.forkhandles.result4k.*
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import tictactoe4k.Player.X

class TicTacToeAppTests {
    private val app = TicTacToeApp()

    @Test fun `game can be looked up by id`() {
        val game = Game()
        val id = app.add(game)
        expectThat(app.findGame(id)).isEqualTo(Success(game))
    }

    @Test fun `added games have different ids`() {
        val id1 = app.add(Game())
        val id2 = app.add(Game())
        expectThat(id1).isNotEqualTo(id2)
    }

    @Test fun `games are updated independently`() {
        val id1 = app.add(Game())
        val id2 = app.add(Game())

        app.makeMove(id1, 0, 0).expectSuccess()
        app.makeMove(id2, 1, 1).expectSuccess()

        expectThat(app.findGame(id1)).isEqualTo(Game(listOf(Move(0, 0, X))).asSuccess())
        expectThat(app.findGame(id2)).isEqualTo(Game(listOf(Move(1, 1, X))).asSuccess())
    }

    @Test fun `can't find non-existent game`() {
        expectThat(app.findGame(GameId("non-existent-id")))
            .isEqualTo(GameNotFound(GameId("non-existent-id")).asFailure())
    }

    @Test fun `can't make move in non-existent game`() {
        expectThat(app.makeMove(GameId("non-existent-id"), 0, 0))
            .isEqualTo(GameNotFound(GameId("non-existent-id")).asFailure())
    }
}

@IgnorableReturnValue
private fun <E> Result4k<*, E>.expectFailure(): Failure<E> =
    when (this) {
        is Success -> throw AssertionError("Expected failure but was $this")
        is Failure -> this
    }

@IgnorableReturnValue fun <T> Result4k<T, *>.expectSuccess(): T =
    when (this) {
        is Success -> this.value
        is Failure -> throw AssertionError("Expected success but was $this")
    }
