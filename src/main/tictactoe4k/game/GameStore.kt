package tictactoe4k.game

interface GameStore {
    fun newGame(): GameId
    fun findGame(id: GameId): Game
    fun makeMove(id: GameId, x: Int, y: Int)
    fun update(id: GameId, game: Game)
}

@JvmInline
value class GameId(val value: String) {
    override fun toString() = value
}

data class GameNotFound(val id: GameId) : GameException("Game not found: $id")

