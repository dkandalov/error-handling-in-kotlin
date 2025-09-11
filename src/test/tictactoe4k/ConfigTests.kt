package tictactoe4k

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

class ConfigTests {
    @Test fun `defaults when map is empty`() {
        expectThat(Config(emptyMap())).isEqualTo(
            Config(
                port = 8080,
                useInMemoryStore = true,
                jdbcUrl = "jdbc:h2:file:./data",
            )
        )
    }

    @Test fun `values are parsed from map`() {
        expectThat(
            Config(
                mapOf(
                    "PORT" to "1234",
                    "IN_MEMORY_STORE" to "true",
                    "JDBC_DATABASE_URL" to "jdbc:h2:mem:testdb",
                )
            )
        ).isEqualTo(
            Config(
                port = 1234,
                useInMemoryStore = true,
                jdbcUrl = "jdbc:h2:mem:testdb",
            )
        )
    }

    @Test fun `invalid port`() {
        assertThrows<NumberFormatException> {
            Config(mapOf("PORT" to "eighty-eighty"))
        }
    }

    @Test fun `invalid useInMemoryStore`() {
        expectThat(Config(mapOf("IN_MEMORY_STORE" to "not-a-boolean")).useInMemoryStore).isFalse()
    }

    @Test fun `invalid h2JdbcUrl`() {
        expectThat(Config(mapOf("JDBC_DATABASE_URL" to "not-a-url")).jdbcUrl).isEqualTo("not-a-url")
    }
}
