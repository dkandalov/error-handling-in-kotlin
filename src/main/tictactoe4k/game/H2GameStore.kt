package tictactoe4k.game

class H2GameStore(private val jdbcUrl: String) : GameStore, AutoCloseable {
    fun init() = apply { TODO() }

    override fun newGame() = TODO()

    override fun findGame(id: GameId) = TODO()

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId) = TODO()

    override fun newUserId() = TODO()

    override fun close() = TODO()
}