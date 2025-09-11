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

    WebApp(gameStore).startOn(config.port)

    println("Started web server on http://localhost:${config.port}")
}

private fun WebApp.startOn(port: Int) {
    poly(httpHandler, sseHandler)
        .asServer(Jetty11(port))
        .start()
}

private data class Config(
    val port: Int,
    val useInMemoryStore: Boolean,
    val h2JdbcUrl: String,
) {
    constructor(map: Map<String, String> = System.getenv()) : this(
        port = map["PORT"]?.toInt() ?: 8080,
        useInMemoryStore = map["IN_MEMORY_STORE"]?.toBoolean() ?: true,
        h2JdbcUrl = map["JDBC_DATABASE_URL"] ?: "jdbc:h2:file:./data",
    )
}