package tictactoe4k

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import tictactoe4k.Player.X

class H2GameStoreTests {
    private val jdbcUrl = "jdbc:h2:mem:tictactoe_test;DB_CLOSE_DELAY=-1"

    @Test fun `persists games across store instances`() {
        val store1 = H2GameStore(jdbcUrl)
        val id = store1.add(Game())
        store1.makeMove(id, 0, 1)

        val store2 = H2GameStore(jdbcUrl)
        expectThat(store2.findBy(id)).isEqualTo(Game(listOf(Move(0, 1, X))))
    }
}
