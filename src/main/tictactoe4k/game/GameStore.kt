package tictactoe4k.game

import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface GameStore {
    fun findBy(id: GameId): Game
    fun makeMove(id: GameId, x: Int, y: Int)
    fun update(id: GameId, game: Game)
    fun add(game: Game): GameId
}

@JvmInline
value class GameId(val value: String) {
    override fun toString() = value
}

data class GameNotFound(val id: GameId) : GameException("Game not found: $id")

class InMemoryGameStore(
    private val gamesById: MutableMap<GameId, Game> = ConcurrentHashMap(),
    private val generateId: () -> GameId = { GameId(UUID.randomUUID().toString()) },
) : GameStore {
    override fun findBy(id: GameId): Game =
        gamesById[id] ?: throw GameNotFound(id)

    override fun makeMove(id: GameId, x: Int, y: Int) {
        val updatedGame = findBy(id).makeMove(x, y)
        update(id, updatedGame)
    }

    override fun update(id: GameId, game: Game) {
        if (id !in gamesById.keys) throw GameNotFound(id)
        gamesById[id] = game
    }

    override fun add(game: Game): GameId {
        val id = generateId()
        gamesById[id] = game
        return id
    }
}
