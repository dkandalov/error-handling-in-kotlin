package tictactoe4k

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.sse.SseMessage
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import tictactoe4k.game.*
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(ApprovalTest::class)
class WebAppTests {
    private val gameId = GameId("some-game-id")
    private val gameStore = InMemoryGameStore(
        gamesById = mutableMapOf(gameId to Game()),
        generateId = sequentialIds()
    )
    val webApp = WebApp(gameStore)
    private val http = FollowRedirects().then(webApp.httpHandler)
    private val sse = webApp.sseHandler

    @Test fun `create new game`(approver: Approver) {
        val response = http(Request(GET, "/")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `get game state`(approver: Approver) {
        val response = http(Request(GET, "/game/$gameId")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `can't get non-existent game`(approver: Approver) {
        val response = http(Request(GET, "/game/non-existent-id"))
        approver.assertApproved(response)
    }

    @Test fun `players take turns on each move`(approver: Approver) {
        http(Request(GET, "/game/$gameId/move?x=0&y=1")).expectOK()
        val response = http(Request(GET, "/game/$gameId/move?x=2&y=0")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `player X wins`(approver: Approver) {
        gameStore.makeMoves(gameId, playerXWinningMoves)
        approver.assertApproved(http(Request(GET, "/game/$gameId")).expectOK())
    }

    @Test fun `game ends in a draw`(approver: Approver) {
        gameStore.makeMoves(gameId, gameEndsInDrawMoves)
        approver.assertApproved(http(Request(GET, "/game/$gameId")).expectOK())
    }

    @Test fun `can't make moves after game is over`(approver: Approver) {
        gameStore.makeMoves(gameId, playerXWinningMoves)
        approver.assertApproved(http(Request(GET, "/game/$gameId/move?x=1&y=1").cookie("userId", "user-O")).expectOK())
    }

    @Test fun `third player joining the game can't make moves`(approver: Approver) {
        http(Request(GET, "/game/$gameId/move?x=0&y=0")).expectOK()
        http(Request(GET, "/game/$gameId/move?x=1&y=1")).expectOK()
        approver.assertApproved(http(Request(GET, "/game/$gameId/move?x=2&y=2")).expectOK())
    }

    @Test fun `duplicate move`(approver: Approver) {
        http(Request(GET, "/game/$gameId/move?x=1&y=1")).expectOK()
        approver.assertApproved(http(Request(GET, "/game/$gameId/move?x=1&y=1")).expectOK())
    }

    @Test fun `same player makes two moves in a row`(approver: Approver) {
        http(Request(GET, "/game/$gameId/move?x=1&y=1").cookie("userId", "user-O")).expectOK()
        approver.assertApproved(http(Request(GET, "/game/$gameId/move?x=2&y=2").cookie("userId", "user-O")).expectOK())
    }

    @Test fun `out of range moves`(approver: Approver) {
        approver.assertApproved(http(Request(GET, "/game/$gameId/move?x=-123&y=234")).expectOK())
    }

    @Test fun `missing arguments on move request`(approver: Approver) {
        approver.assertApproved(http(Request(GET, "/game/$gameId/move")).expectOK())
    }

    @Test fun `invalid arguments on move request`(approver: Approver) {
        approver.assertApproved(http(Request(GET, "/game/$gameId/move?x=foo&y=bar")).expectOK())
    }

    @Test fun `receive SSE updates`() {
        val sseClient = webApp.sseHandler.testSseClient(Request(GET, "/game/$gameId/events?gameId=$gameId"))
        http(Request(GET, "/game/$gameId/move?x=0&y=0")).expectOK()
        http(Request(GET, "/game/$gameId/move?x=0&y=1")).expectOK()

        expectThat(sseClient.received().toList()).isEqualTo(
            listOf(
                SseMessage.Event("connected", gameId.value),
                SseMessage.Event("update", gameId.value),
                SseMessage.Event("update", gameId.value),
            )
        )
    }

    @Test fun `receive SSE updates for the new game when it's started`() {
        val sseClient = webApp.sseHandler.testSseClient(Request(GET, "/game/$gameId/events?gameId=$gameId"))
        http(Request(GET, "/?gameId=$gameId")).expectOK()

        expectThat(sseClient.received().toList()).isEqualTo(
            listOf(
                SseMessage.Event("connected", gameId.value),
                SseMessage.Event("update", "2"),
            )
        )
    }

    @Test fun `can't subscribe to SSE if the game doesn't exist`() {
        val sseClient = webApp.sseHandler.testSseClient(Request(GET, "/game/non-existent-id/events"))
        expectThat(sseClient.status).isEqualTo(NOT_FOUND)
    }

    private fun Response.expectOK(): Response {
        expectThat(status).isEqualTo(OK)
        return this
    }

    private fun GameStore.makeMoves(id: GameId, moves: List<Move>) {
        val alternateUsers = generateSequence { sequenceOf(UserId("user-X"), UserId("user-O")) }.flatten()
        moves.asSequence().zip(alternateUsers).forEach { (move, user) ->
            makeMove(id, move.x, move.y, user)
        }
    }
}

fun sequentialIds(): () -> String {
    val id = AtomicInteger(0)
    return { id.incrementAndGet().toString() }
}
