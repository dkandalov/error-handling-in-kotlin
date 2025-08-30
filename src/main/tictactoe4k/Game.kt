package tictactoe4k

import tictactoe4k.Player.O
import tictactoe4k.Player.X

data class Game(val moves: List<Move> = emptyList()) {
    fun makeMove(x: Int, y: Int): Game {
        if (isOver) throw MoveAfterGameOver()
        if (x !in 0..2 || y !in 0..2) throw OutOfRangeMove(x, y)
        if (moves.any { it.x == x && it.y == y }) throw DuplicateMove(x, y)

        val nextPlayer = if (moves.lastOrNull()?.player == X) O else X
        return Game(moves + Move(x, y, nextPlayer))
    }

    val winner: Player? = findWinner()
    val isOver = winner != null || moves.size == 9

    private fun findWinner(): Player? =
        Player.entries.find { player ->
            (0..2).all { Move(it, 0, player) in moves } ||
            (0..2).all { Move(it, 1, player) in moves } ||
            (0..2).all { Move(it, 2, player) in moves } ||
            (0..2).all { Move(0, it, player) in moves } ||
            (0..2).all { Move(1, it, player) in moves } ||
            (0..2).all { Move(2, it, player) in moves } ||
            (0..2).all { Move(it, it, player) in moves } ||
            (0..2).all { Move(it, 2 - it, player) in moves }
        }
}

data class Move(
    val x: Int,
    val y: Int,
    val player: Player,
)

enum class Player { X, O }

open class GameException(message: String? = null) : Exception(message)
class MoveAfterGameOver : GameException("You can't move after the game is over")
data class OutOfRangeMove(val x: Int, val y: Int) : GameException("Out of range: x=$x, y=$y")
data class DuplicateMove(val x: Int, val y: Int) : GameException("Duplicate move at x=$x, y=$y")
