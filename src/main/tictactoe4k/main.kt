package tictactoe4k

import org.http4k.routing.poly
import org.http4k.server.Jetty11
import org.http4k.server.asServer
import tictactoe4k.game.H2GameStore
import tictactoe4k.game.InMemoryGameStore

fun main() {
    val config = Config()
    val gameStore =
        if (config.useInMemoryStore) InMemoryGameStore()
        else H2GameStore(config.h2JdbcUrl).init()
    val webApp = WebApp(gameStore)

    poly(webApp.httpHandler, webApp.sseHandler).asServer(Jetty11(port = config.port)).start()
    println("Started web server on http://localhost:${config.port}")
}

data class Config(
    val port: Int = System.getenv("PORT")?.toInt() ?: 8080,
    val useInMemoryStore: Boolean = System.getenv("IN_MEMORY_STORE")?.toBoolean() ?: true,
    val h2JdbcUrl: String = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:h2:file:./data",
)