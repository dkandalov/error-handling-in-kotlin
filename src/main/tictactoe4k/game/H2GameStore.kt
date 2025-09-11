package tictactoe4k.game

import java.util.*

class H2GameStore(
    private val jdbcUrl: String,
    private val generateId: () -> String = { UUID.randomUUID().toString() },
) : GameStore, AutoCloseable {
    fun init() = apply {
        TODO()
    }

    override fun newGame() = TODO()

    override fun findGame(id: GameId) = TODO()

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId) = TODO()

    override fun newUserId() = TODO()

    override fun close() = TODO()
}