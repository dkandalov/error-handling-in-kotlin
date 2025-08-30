package tictactoe4k

import org.http4k.server.Jetty11
import org.http4k.server.asServer

fun main() {
    WebApp().asServer(Jetty11(port = 8080)).start()
    println("Started web server on http://localhost:8080")
}