package tictactoe4k

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import tictactoe4k.game.Game
import tictactoe4k.game.GameId
import tictactoe4k.game.GameNotFound
import tictactoe4k.game.GameStore
import tictactoe4k.game.H2GameStore
import tictactoe4k.game.InMemoryGameStore
import tictactoe4k.game.Move
import tictactoe4k.game.Player.X
import kotlin.test.assertFailsWith

class InMemoryGameStoreTests: GameStoreTests(InMemoryGameStore())

class H2GameStoreTests: GameStoreTests(H2GameStore("jdbc:h2:mem:tictactoe_test;DB_CLOSE_DELAY=-1"))

abstract class GameStoreTests(private val store: GameStore) {

    @Test fun `game can be looked up by id`() {
        val game = Game()
        val id = store.add(game)
        expectThat(store.findBy(id)).isEqualTo(game)
    }

    @Test fun `added games have different ids`() {
        val id1 = store.add(Game())
        val id2 = store.add(Game())
        expectThat(id1).isNotEqualTo(id2)
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `games are updated independently`() {
        val id1 = store.add(Game())
        val id2 = store.add(Game())

        store.makeMove(id1, 0, 0)
        store.makeMove(id2, 1, 1)

        expectThat(store.findBy(id1)).isEqualTo(Game(listOf(Move(0, 0, X))))
        expectThat(store.findBy(id2)).isEqualTo(Game(listOf(Move(1, 1, X))))
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't find non-existent game`() {
        assertFailsWith<GameNotFound> { store.findBy(GameId("non-existent-id")) }
    }

    @Suppress("RETURN_VALUE_NOT_USED")
    @Test fun `can't make move in non-existent game`() {
        assertFailsWith<GameNotFound> { store.makeMove(GameId("non-existent-id"), 0, 0) }
    }
}
