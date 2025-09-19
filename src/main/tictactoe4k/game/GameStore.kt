package tictactoe4k.game

import dev.forkhandles.result4k.Result

interface GameStore {
    fun newGame(): GameId
    fun findGame(id: GameId): Game
    fun newUserId(): UserId
    fun makeMove(id: GameId, x: Int, y: Int, userId: UserId, presenter: Presenter = noopPresenter): Result<Game, GameException>
    fun findGame_new(id: GameId): Result<Game?, GameNotFound>
}

interface Presenter {
    fun success(game: Game)
    fun failure(gameId: GameId)
}

val noopPresenter = object : Presenter {
    override fun success(game: Game) = TODO("Not yet implemented")
    override fun failure(gameId: GameId) {}
}

@JvmInline
value class GameId(val value: String) {
    override fun toString() = value
}

@JvmInline
value class UserId(val value: String) {
    override fun toString() = value
}

data class GameNotFound(val id: GameId) : GameException("Game not found: $id")
data class CannotAddNewPlayer(val id: GameId) : GameException("There are already two players: $id")
