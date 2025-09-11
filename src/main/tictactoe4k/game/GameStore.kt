package tictactoe4k.game

interface GameStore {
    fun newGame(): GameId
    fun findGame(id: GameId): Game
    fun makeMove(id: GameId, x: Int, y: Int, userId: UserId): Game
    fun newUserId(): UserId
}

@JvmInline
value class GameId(val value: String) {
    override fun toString() = value
}

@JvmInline
value class UserId(val value: String) {
    override fun toString() = value
}

open class GameStoreException(message: String? = null) : Exception(message)
data class GameNotFound(val id: GameId) : GameStoreException("Game not found: $id")
data class CannotAddNewPlayer(val id: GameId) : GameStoreException("There are already two players: $id")
