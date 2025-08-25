package tictactoe4k

import org.http4k.server.ApacheServer
import org.http4k.server.asServer

fun main() {
    WebApp(TicTacToeApp()).asServer(ApacheServer(port = 8080)).start()
    println("Started web server on http://localhost:8080")
}