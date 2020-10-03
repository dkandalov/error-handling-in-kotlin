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

@ExtendWith(ApprovalTest::class)
class FrontendTests {
    private val id = "gameId"
    private val gameRepository = GameRepository(
        gamesById = mutableMapOf(id to Game()),
        generateId = generateSequentialIds()
    )
    private val backend = Backend(gameRepository)
    private val frontend = ClientFilters.FollowRedirects().then(
        Frontend(backend)
    )

    @Test fun `create new game`(approver: Approver) {
        val response = frontend(Request(GET, "/")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `get game state`(approver: Approver) {
        val response = frontend(Request(GET, "/game/$id")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `players take turns on each move`(approver: Approver) {
        frontend(Request(GET, "/game/$id/move?x=0&y=1")).expectOK()
        val response = frontend(Request(GET, "/game/$id/move?x=2&y=0")).expectOK()
        approver.assertApproved(response)
    }

    @Test fun `player X wins`(approver: Approver) {
        backend.gameWonByPlayerX(id)
        approver.assertApproved(frontend(Request(GET, "/game/$id")).expectOK())
    }

    @Test fun `game ends in a draw`(approver: Approver) {
        backend.gameEndsInDraw(id)
        approver.assertApproved(frontend(Request(GET, "/game/$id")).expectOK())
    }

    @Test fun `show error page on invalid json`(approver: Approver) {
        val frontend = Frontend(backend = {
            Response(OK).body("invalid json")
        })
        approver.assertApproved(frontend(Request(GET, "/game/$id")).expectOK())
    }
}