package tictactoe4k

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.asResultOr
import tictactoe4k.Player.*
import java.util.*
import kotlin.collections.HashMap

class GameRepository(
    private val gamesById: MutableMap<String, Game> = HashMap(),
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    fun find(gameId: String): Result<Game, GameError> {
        return gamesById[gameId].asResultOr { GameNotFound(gameId) }
    }

    fun update(gameId: String, game: Game): Result<Game, GameError> {
        if (gameId !in gamesById.keys) return Failure(GameNotFound(gameId))
        gamesById[gameId] = game
        return Success(game)
    }

    fun add(game: Game): String {
        val gameId = generateId()
        gamesById[gameId] = game
        return gameId
    }
}

data class Game(val moves: List<Move> = emptyList()) {
    fun makeMove(x: Int, y: Int): Result<Game, GameError> {
        if (isOver) return Failure(MoveAfterGameOver)
        if (x !in 0..2 || y !in 0..2) return Failure(OutOfRangeMove(x, y))
        if (moves.any { it.x == x && it.y == y }) return Failure(DuplicateMove(x, y))

        val nextPlayer = if (moves.lastOrNull()?.player == X) O else X
        return Success(Game(moves + Move(x, y, nextPlayer)))
    }

    val winner: Player? = findWinner()
    val isOver = winner != null || moves.size == 9

    private fun findWinner(): Player? {
        return values().find { player ->
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
}

data class Move(
    val x: Int,
    val y: Int,
    val player: Player,
)

enum class Player { X, O }

sealed class GameError
object MoveAfterGameOver : GameError()
data class OutOfRangeMove(val x: Int, val y: Int) : GameError()
data class DuplicateMove(val x: Int, val y: Int) : GameError()
data class GameNotFound(val gameId: String) : GameError()
