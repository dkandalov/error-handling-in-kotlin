package tictactoe4k

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import tictactoe4k.game.Game
import tictactoe4k.game.GameId
import tictactoe4k.game.GameStore
import tictactoe4k.game.InMemoryGameStore
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(ApprovalTest::class)
class WebAppTests {
    private val id = GameId("some-game-id")
    private val gameStore = InMemoryGameStore(
        gamesById = mutableMapOf(id to Game()),
        generateId = sequentialIds()
    )
    private val webApp = ClientFilters.FollowRedirects().then(
        WebApp(gameStore)
    )

    @Test fun `create new game`(approver: Approver) {
        val response = webApp(Request(GET, "/")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `get game state`(approver: Approver) {
        val response = webApp(Request(GET, "/game/$id")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `can't get non-existent game`(approver: Approver) {
        val response = webApp(Request(GET, "/game/non-existent-id"))
        approver.assertApproved(response)
    }

    @Test fun `players take turns on each move`(approver: Approver) {
        webApp(Request(GET, "/game/$id/move?x=0&y=1")).expectOK()
        val response = webApp(Request(GET, "/game/$id/move?x=2&y=0")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `player X wins`(approver: Approver) {
        gameStore.makeMoves(id, playerXWinningMoves)
        approver.assertApproved(webApp(Request(GET, "/game/$id")).expectOK())
    }

    @Test fun `game ends in a draw`(approver: Approver) {
        gameStore.makeMoves(id, gameEndsInDrawMoves)
        approver.assertApproved(webApp(Request(GET, "/game/$id")).expectOK())
    }

    @Test fun `can't make moves after game is over`(approver: Approver) {
        gameStore.makeMoves(id, playerXWinningMoves)
        approver.assertApproved(webApp(Request(GET, "/game/$id/move?x=1&y=1")).expectOK())
    }

    @Test fun `duplicate move`(approver: Approver) {
        webApp(Request(GET, "/game/$id/move?x=1&y=1")).expectOK()
        approver.assertApproved(webApp(Request(GET, "/game/$id/move?x=1&y=1")).expectOK())
    }

    @Test fun `out of range moves`(approver: Approver) {
        approver.assertApproved(webApp(Request(GET, "/game/$id/move?x=-123&y=234")).expectOK())
    }

    @Test fun `missing arguments on move request`(approver: Approver) {
        approver.assertApproved(webApp(Request(GET, "/game/$id/move")).expectOK())
    }

    @Test fun `invalid arguments on move request`(approver: Approver) {
        approver.assertApproved(webApp(Request(GET, "/game/$id/move?x=foo&y=bar")).expectOK())
    }

    @IgnorableReturnValue
    private fun Response.expectOK(): Response {
        expectThat(status).isEqualTo(OK)
        return this
    }

    private fun GameStore.makeMoves(id: GameId, moves: List<Pair<Int, Int>>) =
        moves.forEach { (x, y) -> makeMove(id, x, y) }
}

fun sequentialIds(): () -> GameId {
    val id = AtomicInteger(0)
    return { GameId(id.incrementAndGet().toString()) }
}
