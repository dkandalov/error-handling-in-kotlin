package tictactoe4k

import org.http4k.server.Jetty11
import org.http4k.server.asServer
import tictactoe4k.game.InMemoryGameStore

fun main() {
    WebApp(InMemoryGameStore()).asServer(Jetty11(port = 8080)).start()
    println("Started web server on http://localhost:8080")
}