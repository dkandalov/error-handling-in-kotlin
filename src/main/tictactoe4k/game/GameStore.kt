package tictactoe4k.game

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

