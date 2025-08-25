package tictactoe4k

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.forkhandles.result4k.*
import tictactoe4k.Player.O
import tictactoe4k.Player.X

data class Game(val moves: List<Move> = emptyList()) {
    fun makeMove(x: Int, y: Int): Result4k<Game, GameError> {
        if (isOver) return MoveAfterGameOver.asFailure()
        if (x !in 0..2 || y !in 0..2) return OutOfRangeMove(x, y).asFailure()
        if (moves.any { it.x == x && it.y == y }) return DuplicateMove(x, y).asFailure()

        val nextPlayer = if (moves.lastOrNull()?.player == X) O else X
        return Game(moves + Move(x, y, nextPlayer)).asSuccess()
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

sealed class GameError
data object MoveAfterGameOver : GameError()
data class OutOfRangeMove(val x: Int, val y: Int) : GameError()
data class DuplicateMove(val x: Int, val y: Int) : GameError()

fun <T, E> Result4k<T, E>.toEither(): Either<E, T> =
    when (this) {
        is Failure<E> -> reason.left()
        is Success<T> -> value.right()
    }
