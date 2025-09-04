package tictactoe4k

import org.http4k.template.ViewModel
import tictactoe4k.game.Game
import tictactoe4k.game.GameId

fun GameView(game: Game, gameId: GameId) =
    GameView(
        gameId = gameId.value,
        rows = (0..2).map { x ->
            (0..2).map { y ->
                val player = game.moves.find { it.x == x && it.y == y }?.player?.name
                CellView(x, y, player)
            }
        },
        winner = game.winner?.name,
        isOver = game.isOver
    )

class GameView(
    val gameId: String,
    val rows: List<List<CellView>>,
    val winner: String?,
    val isOver: Boolean,
) : ViewModel

class CellView(val x: Int, val y: Int, val player: String?)

class ErrorView(val message: String, val gameId: String? = null) : ViewModel
