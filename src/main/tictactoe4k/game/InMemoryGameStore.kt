package tictactoe4k.game

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameStore(
    private val gamesById: MutableMap<GameId, Game> = ConcurrentHashMap(),
    private val generateId: () -> String = { UUID.randomUUID().toString() },
) : GameStore {
    private val playersByGame = ConcurrentHashMap<GameId, ConcurrentHashMap<UserId, Player>>()

    override fun newGame(): GameId {
        val id = GameId(generateId())
        gamesById[id] = Game()
        return id
    }

    override fun findGame(id: GameId): Game =
        gamesById[id] ?: throw GameNotFound(id)

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId) {
        val players = playersByGame.getOrPut(id) { ConcurrentHashMap() }
        val player = players.getOrPut(userId) {
            when (players.size) {
                0 -> Player.X
                1 -> Player.O
                else -> throw GameException("Cannot make the move. There are already two players.")
            }
        }
        val updatedGame = findGame(id).makeMove(Move(x, y, player))
        gamesById[id] = updatedGame
    }

    override fun newUserId() =
        UserId(generateId())
}