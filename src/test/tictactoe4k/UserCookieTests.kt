package tictactoe4k

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import tictactoe4k.game.InMemoryGameStore

class UserCookieTests {
    private val webApp = WebApp(InMemoryGameStore(generateId = sequentialIds()))

    @Test fun `sets userid cookie when absent`() {
        val response = webApp(Request(GET, "/foo"))
        expectThat(response.cookies()).isEqualTo(listOf(Cookie("userid", "1", path = "/", httpOnly = true)))
    }

    @Test fun `does not overwrite existing userid cookie`() {
        val response = webApp(Request(GET, "/").cookie("userid", "123"))
        expectThat(response.cookies()).isEqualTo(emptyList())
    }
}
