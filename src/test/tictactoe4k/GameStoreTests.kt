package tictactoe4k

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import tictactoe4k.game.*
import tictactoe4k.game.Player.O
import tictactoe4k.game.Player.X
import kotlin.test.assertFailsWith

class InMemoryGameStoreTests: GameStoreTests(InMemoryGameStore())

class H2GameStoreTests: GameStoreTests(H2GameStore("jdbc:h2:mem:tictactoe_test;DB_CLOSE_DELAY=-1").init())

abstract class GameStoreTests(private val store: GameStore) {

    @Test fun `game can be looked up by id`() {
        val id = store.newGame()
        expectThat(store.findGame(id)).isEqualTo(Game())
    }

    @Test fun `games have different ids`() {
        val id1 = store.newGame()
        val id2 = store.newGame()
        expectThat(id1).isNotEqualTo(id2)
    }

    @Test fun `users take turns in the game`() {
        val id = store.newGame()

        expectThat(store.makeMove(id, 0, 0, UserId("user-1")))
            .isEqualTo(Game(listOf(
                Move(0, 0, X)
            )))

        expectThat(store.makeMove(id, 1, 1, UserId("user-2")))
            .isEqualTo(Game(listOf(
                Move(0, 0, X),
                Move(1, 1, O)
            )))

        expectThat(store.makeMove(id, 2, 2, UserId("user-1")))
            .isEqualTo(Game(listOf(
                Move(0, 0, X),
                Move(1, 1, O),
                Move(2, 2, X)
            )))
    }

    @Test fun `games are updated independently`() {
        val id1 = store.newGame()
        val id2 = store.newGame()

        expectThat(store.makeMove(id1, 0, 0, UserId("some-user")))
            .isEqualTo(Game(listOf(Move(0, 0, X))))

        expectThat(store.makeMove(id2, 1, 1, UserId("some-user")))
            .isEqualTo(Game(listOf(Move(1, 1, X))))
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't find non-existent game`() {
        assertFailsWith<GameNotFound> {
            store.findGame(GameId("non-existent-id"))
        }
    }

    @Test fun `can't make move in non-existent game`() {
        assertFailsWith<GameNotFound> {
            val _ = store.makeMove(GameId("non-existent-id"), 0, 0, UserId("some-user"))
        }
    }
}
