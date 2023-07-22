package tictactoe4k

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import tictactoe4k.Player.*
import java.util.*

class GameRepository(
    private val gamesById: MutableMap<String, Game> = HashMap(),
    private val generateId: () -> String = { UUID.randomUUID().toString() },
) {
    fun find(gameId: String): Either<GameError, Game> =
        gamesById[gameId]?.right() ?: GameNotFound(gameId).left()

    fun update(gameId: String, game: Game): Either<GameError, Game> {
        if (gameId !in gamesById.keys) return Left(GameNotFound(gameId))
        gamesById[gameId] = game
        return Right(game)
    }

    fun add(game: Game): String {
        val gameId = generateId()
        gamesById[gameId] = game
        return gameId
    }
}

data class Game(val moves: List<Move> = emptyList()) {
    fun makeMove(x: Int, y: Int): Either<GameError, Game> {
        if (isOver) return Left(MoveAfterGameOver)
        if (x !in 0..2 || y !in 0..2) return Left(OutOfRangeMove(x, y))
        if (moves.any { it.x == x && it.y == y }) return Left(DuplicateMove(x, y))

        val nextPlayer = if (moves.lastOrNull()?.player == X) O else X
        return Right(Game(moves + Move(x, y, nextPlayer)))
    }

    val winner: Player? = findWinner()
    val isOver = winner != null || moves.size == 9

    private fun findWinner(): Player? =
        entries.find { player ->
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
data class GameNotFound(val gameId: String) : GameError()
