package tictactoe4k

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import tictactoe4k.Player.X
import kotlin.test.assertFailsWith

class TicTacToeAppTests {
    private val app = TicTacToeApp()

    @Test fun `game can be looked up by id`() {
        val game = Game()
        val id = app.add(game)
        expectThat(app.findGame(id)).isEqualTo(game)
    }

    @Test fun `added games have different ids`() {
        val id1 = app.add(Game())
        val id2 = app.add(Game())
        expectThat(id1).isNotEqualTo(id2)
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `games are updated independently`() {
        val id1 = app.add(Game())
        val id2 = app.add(Game())

        app.makeMove(id1, 0, 0)
        app.makeMove(id2, 1, 1)

        expectThat(app.findGame(id1)).isEqualTo(Game(listOf(Move(0, 0, X))))
        expectThat(app.findGame(id2)).isEqualTo(Game(listOf(Move(1, 1, X))))
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't find non-existent game`() {
        assertFailsWith<GameNotFound> { app.findGame(GameId("non-existent-id")) }
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make move in non-existent game`() {
        assertFailsWith<GameNotFound> { app.makeMove(GameId("non-existent-id"), 0, 0) }
    }
}
