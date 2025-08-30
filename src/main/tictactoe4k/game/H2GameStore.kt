package tictactoe4k.game

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.*

class H2GameStore(
    private val jdbcUrl: String,
    private val generateId: () -> GameId = { GameId(UUID.randomUUID().toString()) },
) : GameStore {
    fun init() = apply {
        useConnection { connection ->
            connection.createStatement().use { st ->
                st.executeUpdate(
                    """
                    create table if not exists games (
                        id varchar(100) primary key
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    create table if not exists moves (
                        game_id varchar(100) not null,
                        seq int not null,
                        x int not null,
                        y int not null,
                        player varchar(1) not null,
                        primary key (game_id, seq),
                        foreign key (game_id) references games(id) on delete cascade
                    )
                    """.trimIndent()
                )
            }
        }
    }

    override fun newGame(): GameId {
        val id = generateId()
        val game = Game()
        useConnection { connection ->
            try {
                connection.prepareStatement("insert into games(id) values (?)").use { ps ->
                    ps.setString(1, id.value)
                    ps.executeUpdate()
                }
                if (game.moves.isNotEmpty()) {
                    connection.prepareStatement(
                        "insert into moves(game_id, seq, x, y, player) values (?, ?, ?, ?, ?)"
                    ).use { ps ->
                        game.moves.forEachIndexed { index, move ->
                            ps.setString(1, id.value)
                            ps.setInt(2, index)
                            ps.setInt(3, move.x)
                            ps.setInt(4, move.y)
                            ps.setString(5, move.player.name)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
                connection.commit()
            } catch (e: Exception) {
                throw e
            }
        }
        return id
    }

    override fun findGame(id: GameId): Game {
        ensureGameExists(id)
        val moves = mutableListOf<Move>()
        useConnection { connection ->
            connection.prepareStatement(
                "select x, y, player from moves where game_id = ? order by seq asc"
            ).use { ps ->
                ps.setString(1, id.value)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val x = rs.getInt(1)
                        val y = rs.getInt(2)
                        val player = when (rs.getString(3)) {
                            "X" -> Player.X
                            "O" -> Player.O
                            else -> error("Unknown player")
                        }
                        moves.add(Move(x, y, player))
                    }
                }
            }
        }
        return Game(moves)
    }

    override fun makeMove(id: GameId, x: Int, y: Int) {
        val updatedGame = findGame(id).makeMove(x, y)
        useConnection { connection ->
            try {
                connection.prepareStatement("delete from moves where game_id = ?").use<PreparedStatement, Int> { ps ->
                    ps.setString(1, id.value)
                    ps.executeUpdate()
                }
                connection.prepareStatement(
                    "insert into moves(game_id, seq, x, y, player) values (?, ?, ?, ?, ?)"
                ).use<PreparedStatement, IntArray> { ps ->
                    updatedGame.moves.forEachIndexed<Move> { index, move ->
                        ps.setString(1, id.value)
                        ps.setInt(2, index)
                        ps.setInt(3, move.x)
                        ps.setInt(4, move.y)
                        ps.setString(5, move.player.name)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
                connection.commit()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun ensureGameExists(id: GameId) {
        val exists = useConnection { connection ->
            connection.prepareStatement("select 1 from games where id = ?").use { ps ->
                ps.setString(1, id.value)
                ps.executeQuery().use { rs -> rs.next() }
            }
        }
        if (!exists) throw GameNotFound(id)
    }

    private fun <T> useConnection(block: (Connection) -> T): T =
        DriverManager.getConnection(jdbcUrl).use(block)
}