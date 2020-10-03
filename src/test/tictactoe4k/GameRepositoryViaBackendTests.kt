package tictactoe4k

import datsok.shouldEqual
import datsok.shouldNotEqual
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class GameRepositoryViaBackendTests {
    private val repository = GameRepository()
    private val backend = Backend(repository)

    @Test fun `game can be looked up by id`() {
        val id = backend.addGame().bodyString()
        backend.findGame(id).parseGameJson() shouldEqual Game()
    }

    @Test fun `added games have different ids`() {
        val id1 = backend.addGame().bodyString()
        val id2 = backend.addGame().bodyString()
        id1 shouldNotEqual id2
    }

    @Test fun `games are updated independently`() {
        val id1 = backend.addGame().bodyString()
        val id2 = backend.addGame().bodyString()

        backend.makeMove(id1, 0, 0).expectOK()
        backend.makeMove(id2, 1, 1).expectOK()

        backend.findGame(id1).parseGameJson() shouldEqual Game(listOf(Move(0, 0, Player.X)))
        backend.findGame(id2).parseGameJson() shouldEqual Game(listOf(Move(1, 1, Player.X)))
    }

    @Test fun `game can't be found by non-existent id`() {
        backend.findGame("non-existent-id").expect(Status.BAD_REQUEST, "Game not found id='non-existent-id'")
    }

    @Test fun `game can't be updated by non-existent id`() {
        backend.makeMove("non-existent-id", 0, 0).expect(Status.BAD_REQUEST, "Game not found id='non-existent-id'")
    }
}
