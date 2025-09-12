package tictactoe4k.game

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import tictactoe4k.game.Player.O
import tictactoe4k.game.Player.X

data class Game(val moves: List<Move> = emptyList()) {
    val winner: Player? = findWinner()
    val isOver = winner != null || moves.size == 9
    val nextPlayer = if (isOver) null else if (moves.lastOrNull()?.player == X) O else X

    fun makeMove(move: Move): Game | GameException {
        if (isOver) return MoveAfterGameOver(move)
        if (move.x !in 0..2 || move.y !in 0..2) return OutOfRangeMove(move)
        if (moves.any { it.x == move.x && it.y == move.y }) return DuplicateMove(move)
        if (move.player != nextPlayer) return WrongPlayerMove(move.player)

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

typealias GameException = MoveAfterGameOver | OutOfRangeMove | DuplicateMove | WrongPlayerMove

error class MoveAfterGameOver(val move: Move)
error class OutOfRangeMove(val move: Move)
error class DuplicateMove(val move: Move)
error class WrongPlayerMove(val player: Player)
