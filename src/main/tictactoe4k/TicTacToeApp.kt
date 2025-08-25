package tictactoe4k

import dev.forkhandles.result4k.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TicTacToeApp(
    private val gamesById: MutableMap<GameId, Game> = ConcurrentHashMap(),
    private val generateId: () -> GameId = { GameId(UUID.randomUUID().toString()) },
) {
    fun find(id: GameId): Result4k<Game, GameError> =
        gamesById[id].asResultOr { GameNotFound(id) }

    fun makeMove(id: GameId, x: Int, y: Int): Result4k<Game, GameError> {
        val game = find(id).onFailure { return it }
        val updatedGame = game.makeMove(x, y).onFailure { return it }
        return update(id, updatedGame)
    }

    fun update(id: GameId, game: Game): Result4k<Game, GameError> {
        if (id !in gamesById.keys) return GameNotFound(id).asFailure()
        gamesById[id] = game
        return game.asSuccess()
    }

    fun add(game: Game): GameId {
        val id = generateId()
        gamesById[id] = game
        return id
    }
}

data class GameNotFound(val id: GameId) : GameError()

@JvmInline
value class GameId(val value: String) {
    override fun toString() = value
}
