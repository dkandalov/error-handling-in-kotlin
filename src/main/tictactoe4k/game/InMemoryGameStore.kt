package tictactoe4k.game

import dev.forkhandles.result4k.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameStore(
    private val gamesById: MutableMap<GameId, Game> = ConcurrentHashMap(),
    private val generateId: () -> String = { UUID.randomUUID().toString() },
) : GameStore {
    private val playersByGame = ConcurrentHashMap<GameId, ConcurrentHashMap<Player, UserId>>()

    override fun newGame(): GameId {
        val id = GameId(generateId())
        gamesById[id] = Game()
        return id
    }

    override fun findGame(id: GameId): Game =
        findGame_new(id).orThrow()

    override fun findGame_new(id: GameId): Result<Game, GameNotFound> =
        gamesById[id].asResultOr { GameNotFound(id) }

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId) =
        makeMove_new(id, x, y, userId).orThrow()

    override fun makeMove_new(id: GameId, x: Int, y: Int, userId: UserId): Game | GameException | GameRepositoryException {
        val game = findGame_new(id)
            ?.makeMove(Move(x, y, player))!!

        val players = playersByGame.getOrPut(id) { ConcurrentHashMap() }
        val player = Player.entries
            .firstOrNull { players[it] == null || players[it] == userId }
            ?.also { players[it] = userId }
            ?: return CannotAddNewPlayer(id).asFailure()

        val updatedGame = game.makeMove(Move(x, y, player)).ifError { return it }
        gamesById[id] = updatedGame
        return updatedGame.asSuccess()
    }

    inline fun <T : R, E : Error, R> (T | E).ifError(onError: (E) -> R): R {
        return if (this is Error) onError(this) else this
    }

    override fun newUserId() =
        UserId(generateId())
}