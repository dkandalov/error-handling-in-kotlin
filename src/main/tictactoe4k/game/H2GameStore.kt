package tictactoe4k.game

import java.sql.DriverManager
import java.util.*

class H2GameStore(
    private val jdbcUrl: String,
    private val generateId: () -> String = { UUID.randomUUID().toString() },
) : GameStore, AutoCloseable {
    private val connection by lazy { DriverManager.getConnection(jdbcUrl) }

    fun init() = apply {
        connection.createStatement().use {
            it.executeUpdate(
                """
                create table if not exists games (
                    id varchar(100) primary key
                )
                """.trimIndent()
            )
            it.executeUpdate(
                """
                create table if not exists moves (
                    game_id varchar(100) not null,
                    seq int not null,
                    x int not null,
                    y int not null,
                    player varchar(1) not null,
                    user_id varchar(100) not null,
                    primary key (game_id, seq),
                    foreign key (game_id) references games(id) on delete cascade
                )
                """.trimIndent()
            )
            it.executeUpdate(
                """
                create table if not exists game_users (
                    game_id varchar(100) not null,
                    user_id varchar(100) not null,
                    player varchar(1) not null,
                    primary key (game_id, user_id),
                    foreign key (game_id) references games(id) on delete cascade
                )
                """.trimIndent()
            )
        }
    }

    override fun newGame(): GameId {
        val id = generateId()
        val game = Game()
        val _ = connection.prepareStatement("insert into games(id) values (?)").use {
            it.setString(1, id)
            it.executeUpdate()
        }
        if (game.moves.isNotEmpty()) {
            val _ = connection.prepareStatement(
                "insert into moves(game_id, seq, x, y, player) values (?, ?, ?, ?, ?)"
            ).use {
                game.moves.forEachIndexed { index, move ->
                    it.setString(1, id)
                    it.setInt(2, index)
                    it.setInt(3, move.x)
                    it.setInt(4, move.y)
                    it.setString(5, move.player.name)
                    it.addBatch()
                }
                it.executeBatch()
            }
        }
        connection.commit()
        return GameId(id)
    }

    override fun findGame(id: GameId): Game {
        if (!checkGameExists(id)) throw GameNotFound(id)

        val moves = mutableListOf<Move>()
        connection.prepareStatement(
            "select x, y, player from moves where game_id = ? order by seq asc"
        ).use {
            it.setString(1, id.value)
            it.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val x = resultSet.getInt(1)
                    val y = resultSet.getInt(2)
                    val player = when (resultSet.getString(3)) {
                        "X" -> Player.X
                        "O" -> Player.O
                        else -> error("Unknown player")
                    }
                    moves.add(Move(x, y, player))
                }
            }
        }
        return Game(moves)
    }

    override fun makeMove(id: GameId, x: Int, y: Int, userId: UserId): Game {
        if (!checkGameExists(id)) throw GameNotFound(id)
        val existingPlayer = connection.prepareStatement(
            "select player from game_users where game_id = ? and user_id = ?"
        ).use {
            it.setString(1, id.value)
            it.setString(2, userId.value)
            it.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }
        val player =
            if (existingPlayer != null) {
                when (existingPlayer) {
                    "X" -> Player.X
                    "O" -> Player.O
                    else -> error("Unknown player")
                }
            } else {
                val count = connection.createStatement().use {
                    it.executeQuery("select count(*) from game_users where game_id = '${id.value}'").use { rs ->
                        if (rs.next()) rs.getInt(1) else 0
                    }
                }
                val assignedPlayer = when (count) {
                    0 -> Player.X
                    1 -> Player.O
                    else -> throw GameException("Cannot make the move. There are already two players.")
                }
                val _ = connection.prepareStatement(
                    "insert into game_users(game_id, user_id, player) values (?, ?, ?)"
                ).use {
                    it.setString(1, id.value)
                    it.setString(2, userId.value)
                    it.setString(3, assignedPlayer.name)
                    it.executeUpdate()
                }
                assignedPlayer
            }

        val nextSeq = connection.prepareStatement("select max(seq) from moves where game_id = ?")
            .use {
                it.setString(1, id.value)
                it.executeQuery().use { resultSet ->
                    if (resultSet.next()) resultSet.getInt(1) else -1
                }
            } + 1

        val move = Move(x, y, player)
        val updatedGame = findGame(id).makeMove(move)
        val newMove = updatedGame.moves.last()

        val _ = connection.prepareStatement(
            "insert into moves(game_id, seq, x, y, player, user_id) values (?, ?, ?, ?, ?, ?)"
        ).use {
            it.setString(1, id.value)
            it.setInt(2, nextSeq)
            it.setInt(3, newMove.x)
            it.setInt(4, newMove.y)
            it.setString(5, newMove.player.name)
            it.setString(6, userId.value)
            it.executeUpdate()
        }
        connection.commit()

        return updatedGame
    }

    override fun newUserId() =
        UserId(generateId())

    private fun checkGameExists(gameId: GameId) =
        connection.prepareStatement("select 1 from games where id = ?").use {
            it.setString(1, gameId.value)
            it.executeQuery().use { rs -> rs.next() }
        }

    override fun close() {
        connection.close()
    }
}