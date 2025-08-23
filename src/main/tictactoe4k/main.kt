package tictactoe4k

import org.http4k.client.OkHttp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.server.ApacheServer
import org.http4k.server.asServer

fun main() {
    Backend(GameRepository()).asServer(ApacheServer(port = 1234)).start()
    println("Started backend on http://localhost:1234")

    val backendClient = SetBaseUriFrom(Uri.of("http://localhost:1234")).then(OkHttp())
    Frontend(backendClient).asServer(ApacheServer(port = 8080)).start()
    println("Started frontend on http://localhost:8080")
}