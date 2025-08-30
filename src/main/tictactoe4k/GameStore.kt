package tictactoe4k

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GameStore(
    private val gamesById: MutableMap<GameId, Game> = ConcurrentHashMap(),
    private val generateId: () -> GameId = { GameId(UUID.randomUUID().toString()) },
) {
    fun findBy(id: GameId): Game =
        gamesById[id] ?: throw GameNotFound(id)

    fun makeMove(id: GameId, x: Int, y: Int) {
        val updatedGame = findBy(id).makeMove(x, y)
        update(id, updatedGame)
    }

    fun update(id: GameId, game: Game) {
        if (id !in gamesById.keys) throw GameNotFound(id)
        gamesById[id] = game
    }

    fun add(game: Game): GameId {
        val id = generateId()
        gamesById[id] = game
        return id
    }
}

data class GameNotFound(val id: GameId) : GameException("Game not found: $id")

@JvmInline
value class GameId(val value: String) {
    override fun toString() = value
}
