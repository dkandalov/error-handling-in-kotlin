package tictactoe4k.game

import tictactoe4k.game.Player.O
import tictactoe4k.game.Player.X

data class Game(val moves: List<Move> = emptyList()) {
    val winner: Player? = findWinner()
    val isOver = winner != null || moves.size == 9
    val nextPlayer = if (isOver) null else if (moves.lastOrNull()?.player == X) O else X

    fun makeMove(move: Move): Game = makeMove_(move)

    fun makeMove_(move: Move): Game {
        if (isOver) throw MoveAfterGameOver()
        if (move.x !in 0..2 || move.y !in 0..2) throw OutOfRangeMove(move.x, move.y)
        if (moves.any { it.x == move.x && it.y == move.y }) throw DuplicateMove(move.x, move.y)
        if (move.player != nextPlayer) throw WrongPlayerMove(move.player)

        return Game(moves + move)
    }

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
data class WrongPlayerMove(val player: Player) : GameException("It's not $player's turn")
