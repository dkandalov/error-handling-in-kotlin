package tictactoe4k.game

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameStore(
    private val gamesById: MutableMap<GameId, Game> = ConcurrentHashMap(),
    private val generateId: () -> String = { UUID.randomUUID().toString() },
) : GameStore {
    override fun newGame(): GameId {
        val id = GameId(generateId())
        gamesById[id] = Game()
        return id
    }

    override fun findGame(id: GameId): Game =
        gamesById[id] ?: throw GameNotFound(id)

    override fun makeMove(id: GameId, x: Int, y: Int) {
        val updatedGame = findGame(id).makeMove(x, y)
        gamesById[id] = updatedGame
    }

    override fun newUserId(): String {
        return generateId()
    }
}