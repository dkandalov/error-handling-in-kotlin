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
        gamesById[id]?.asSuccess() ?: GameNotFound(id).asFailure()

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId, presenter: Presenter): Result<Game, GameException> {
        val game = findGame_new(id).onFailure {
            presenter.failure(id)
            return it
        }

        val players = playersByGame.getOrPut(id) { ConcurrentHashMap() }
        val player = Player.entries
            .firstOrNull { players[it] == null || players[it] == userId }
            ?.also { players[it] = userId }
            .asResultOr { CannotAddNewPlayer(id) }.onFailure { return it }

        val updatedGame = game.makeMove(Move(x, y, player)).onFailure { return it }

        gamesById[id] = updatedGame
        return updatedGame.asSuccess()
    }

    override fun newUserId() =
        UserId(generateId())
}