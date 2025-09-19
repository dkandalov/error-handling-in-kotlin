package tictactoe4k

import org.http4k.routing.poly
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty11
import org.http4k.server.asServer
import tictactoe4k.game.H2GameStore
import tictactoe4k.game.InMemoryGameStore
import java.util.concurrent.CopyOnWriteArrayList

fun main() {
    val closable = CompositeClosable().closeOnShutdown()
    val config = Config()
    val gameStore =
        if (config.useInMemoryStore) InMemoryGameStore()
        else H2GameStore(config.jdbcUrl).init().also(closable::add)

    WebApp(gameStore).startOn(config.port).also(closable::add)

    println("Started web server on http://localhost:${config.port}")
}

class CompositeClosable(
    private val closables: MutableList<AutoCloseable> = CopyOnWriteArrayList()
) : AutoCloseable {
    fun add(closable: AutoCloseable) =
        closables.add(closable)

    override fun close() =
        closables.reversed().forEach { it.close() }

    fun closeOnShutdown() = apply {
        Runtime.getRuntime().addShutdownHook(Thread(this::close))
    }
}

private fun WebApp.startOn(port: Port): Http4kServer =
    poly(httpHandler, sseHandler)
        .asServer(Jetty11(port.value))
        .start()

data class Config(
    val port: Port,
    val useInMemoryStore: Boolean,
    val jdbcUrl: String,
) {
    constructor(map: Map<String, String> = System.getenv()) : this(
        port = map["PORT"]?.toInt()?.let(::Port) ?: Port(8080),
        useInMemoryStore = map["IN_MEMORY_STORE"]?.toBoolean() ?: true,
        jdbcUrl = map["JDBC_DATABASE_URL"] ?: "jdbc:h2:file:./data",
    )
}

@JvmInline
value class Port(val value: Int) {
    init {
        require(value in 1..65535) { "Invalid port: $value" }
    }
}








