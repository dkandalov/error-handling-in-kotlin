package tictactoe4k

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
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
        val first = webApp(Request(GET, "/"))
        val setCookie = first.headers.firstNotNullOf { (name, value) -> if (name.equals("Set-Cookie", ignoreCase = true) && value?.startsWith("userid=") == true) value else null }
        val value = setCookie.substringAfter("userid=").substringBefore(';')

        val response = webApp(Request(GET, "/").header("Cookie", "userid=$value"))
        // Should not set a new cookie when one is already present
        val cookies = response.headers.mapNotNull { (name, v) -> if (name.equals("Set-Cookie", ignoreCase = true) && v?.startsWith("userid=") == true) v else null }
        expectThat(cookies.size).isEqualTo(0)
    }
}
