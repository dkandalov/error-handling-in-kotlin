package tictactoe4k.game

import dev.forkhandles.result4k.orThrow
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
        gamesById[id] ?: throw GameNotFound(id)

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId): Game {
        val game = findGame(id)

        val players = playersByGame.getOrPut(id) { ConcurrentHashMap() }
        val player = Player.entries
            .firstOrNull { players[it] == null || players[it] == userId }
            ?.also { players[it] = userId }
            ?: throw CannotAddNewPlayer(id)

        val updatedGame = game.makeMove_new(Move(x, y, player)).orThrow()
        gamesById[id] = updatedGame
        return updatedGame
    }

    override fun newUserId() =
        UserId(generateId())
}