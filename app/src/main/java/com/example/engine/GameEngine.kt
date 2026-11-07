package com.example.engine

import android.os.SystemClock
import android.util.Log
import com.example.model.GameMode
import com.example.model.GamePhase
import com.example.model.GameState
import com.example.model.Player
import com.example.model.QuestionDifficulty
import com.example.model.QuestionDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class GameEngine(
    private val scope: CoroutineScope,
    initialRoomCode: String = "1234",
    hostIp: String = "127.0.0.1",
    initialGameMode: GameMode = GameMode.SQUAD
) {
    private val tag = "GameEngine"
    private val mutex = Mutex()

    private val _state = MutableStateFlow(
        GameState(
            roomCode = initialRoomCode,
            hostIp = hostIp,
            gameMode = initialGameMode,
            phase = GamePhase.WAITING_FOR_PLAYERS
        )
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var fastestBuzzerArrivalNanos: Long = Long.MAX_VALUE

    suspend fun setGameMode(mode: GameMode) {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.WAITING_FOR_PLAYERS && current.phase != GamePhase.READY) {
                return@withLock
            }
            val required = mode.maxPlayers
            val newPhase = if (current.players.size >= required) GamePhase.READY else GamePhase.WAITING_FOR_PLAYERS
            _state.value = current.copy(
                gameMode = mode,
                phase = newPhase,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Game mode changed to: ${mode.name} (required: $required players)")
        }
    }

    suspend fun addPlayer(playerId: String, playerName: String, isHost: Boolean): Pair<Boolean, String?> {
        return mutex.withLock {
            val current = _state.value
            val existing = current.players.find { it.id == playerId }
            if (existing != null) {
                val updatedPlayers = current.players.map {
                    if (it.id == playerId) it.copy(isConnected = true, name = playerName) else it
                }
                _state.value = current.copy(
                    players = updatedPlayers,
                    stateVersion = current.stateVersion + 1
                )
                Log.d(tag, "Existing player reconnected: $playerName ($playerId)")
                return@withLock Pair(true, null)
            }

            val maxAllowed = current.gameMode.maxPlayers
            if (current.players.size >= maxAllowed) {
                return@withLock Pair(false, "الغرفة مكتملة بالفعل ($maxAllowed لاعبين)")
            }

            val assignedColor = current.players.size
            val newPlayer = Player(
                id = playerId,
                name = playerName.ifBlank { "لاعب ${current.players.size + 1}" },
                score = 0,
                isHost = isHost,
                isConnected = true,
                colorIndex = assignedColor
            )
            val updatedList = current.players + newPlayer
            val newPhase = if (updatedList.size >= maxAllowed) GamePhase.READY else GamePhase.WAITING_FOR_PLAYERS

            val firstQuestionerId = if (updatedList.size >= maxAllowed && current.currentQuestionerId == null) {
                updatedList.first().id
            } else {
                current.currentQuestionerId
            }

            _state.value = current.copy(
                players = updatedList,
                phase = newPhase,
                currentQuestionerId = firstQuestionerId,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Player joined: ${newPlayer.name}. Total: ${updatedList.size}/$maxAllowed")
            Pair(true, null)
        }
    }

    suspend fun onPlayerDisconnected(playerId: String) {
        mutex.withLock {
            val current = _state.value
            val player = current.players.find { it.id == playerId } ?: return@withLock
            val updatedPlayers = current.players.map {
                if (it.id == playerId) it.copy(isConnected = false) else it
            }

            Log.w(tag, "Player marked offline (can reconnect): ${player.name}")

            // If player was actively answering and disconnected during game play
            if (current.phase == GamePhase.ANSWERING && current.currentAnswererId == playerId) {
                timerJob?.cancel()
                val newDisqualified = current.disqualifiedIds + playerId
                val remaining = updatedPlayers.filter {
                    it.id != current.currentQuestionerId && it.id !in newDisqualified && it.isConnected
                }
                if (remaining.isNotEmpty()) {
                    val nextAnswerer = remaining.random()
                    _state.value = current.copy(
                        players = updatedPlayers,
                        currentAnswererId = nextAnswerer.id,
                        disqualifiedIds = newDisqualified,
                        remainingSeconds = current.duration.totalSeconds,
                        stateVersion = current.stateVersion + 1
                    )
                    startAnswerCountdown(current.duration.totalSeconds)
                } else {
                    handleAllCompetitorsFailed(current.copy(players = updatedPlayers, disqualifiedIds = newDisqualified))
                }
            } else {
                _state.value = current.copy(
                    players = updatedPlayers,
                    stateVersion = current.stateVersion + 1
                )
            }
        }
    }

    suspend fun startWheelSpin() {
        mutex.withLock {
            val current = _state.value
            val targetCount = current.gameMode.maxPlayers
            if (current.players.size < targetCount) return@withLock

            val chosenPlayer: Player
            val chosenIndex: Int

            if (current.gameMode == GameMode.DUO) {
                // In DUO mode: Alternate between the two players
                val nextQuestioner = current.players.find { it.id != current.currentQuestionerId }
                    ?: current.players.first()
                chosenPlayer = nextQuestioner
                chosenIndex = current.players.indexOfFirst { it.id == nextQuestioner.id }.coerceAtLeast(0)
            } else {
                // TRIO & SQUAD: Realistic random selection among all players
                chosenIndex = Random.nextInt(current.players.size)
                chosenPlayer = current.players[chosenIndex]
            }

            val segmentDegrees = 360f / current.players.size
            // Natural random variance within the winning slice (-25% to +25% of slice)
            val jitter = (Random.nextFloat() * 0.5f - 0.25f) * segmentDegrees
            // Align chosen slice center with top pointer (270 degrees in Android Canvas)
            val targetOffset = (chosenIndex * segmentDegrees) + (segmentDegrees / 2f) + jitter
            val targetInOneCircle = (270f - targetOffset).mod(360f)
            val totalRotations = (5..8).random() * 360f
            val finalAngle = totalRotations + targetInOneCircle

            _state.value = current.copy(
                phase = GamePhase.SPINNING_WHEEL,
                wheelTargetPlayerId = chosenPlayer.id,
                wheelTargetAngle = finalAngle,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Spinning wheel. Winner: ${chosenPlayer.name} (index $chosenIndex) at angle: $finalAngle")
        }
    }

    suspend fun completeWheelSpin() {
        mutex.withLock {
            val current = _state.value
            // Prevent duplicate completion calls from multiple devices
            if (current.phase != GamePhase.SPINNING_WHEEL) {
                Log.d(tag, "completeWheelSpin ignored because phase is already: ${current.phase}")
                return@withLock
            }
            val newQuestionerId = current.wheelTargetPlayerId
                ?: current.currentQuestionerId
                ?: return@withLock

            // Candidates to answer are all players except the designated Questioner
            val candidatePlayers = current.players.filter { it.id != newQuestionerId }
            // Shuffle to prevent repeating pattern
            val shuffledIds = candidatePlayers.map { it.id }.shuffled()

            _state.value = current.copy(
                phase = GamePhase.CARD_SELECTION,
                currentQuestionerId = newQuestionerId,
                currentAnswererId = null,
                buzzerWinnerId = null,
                disqualifiedIds = emptySet(),
                wheelTargetPlayerId = null,
                cardPlayerIds = shuffledIds,
                selectedCardIndex = null,
                selectedCardPlayerId = null,
                cardsFlippedToBack = false,
                stateVersion = current.stateVersion + 1
            )
            val questionerPlayer = current.players.find { it.id == newQuestionerId }
            Log.d(tag, "Wheel completed successfully. Official Questioner: ${questionerPlayer?.name} ($newQuestionerId). ${shuffledIds.size} cards prepared.")
        }
    }

    suspend fun onCardsFlippedToBack() {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.CARD_SELECTION) return@withLock
            _state.value = current.copy(
                cardsFlippedToBack = true,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Cards flipped to back side.")
        }
    }

    suspend fun onCardSelected(cardIndex: Int, senderPlayerId: String? = null) {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.CARD_SELECTION) return@withLock
            if (senderPlayerId != null && senderPlayerId != current.currentQuestionerId) {
                Log.w(tag, "Unauthorized card selection rejected from non-questioner: $senderPlayerId")
                return@withLock
            }
            if (cardIndex !in current.cardPlayerIds.indices) return@withLock
            if (current.selectedCardIndex != null) return@withLock

            val chosenPlayerId = current.cardPlayerIds[cardIndex]
            val chosenPlayer = current.players.find { it.id == chosenPlayerId }

            _state.value = current.copy(
                selectedCardIndex = cardIndex,
                selectedCardPlayerId = chosenPlayerId,
                currentAnswererId = chosenPlayerId,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Card $cardIndex selected by questioner. Chosen answerer: ${chosenPlayer?.name} ($chosenPlayerId)")
        }
    }

    suspend fun onAnswerGiveUp(playerId: String) {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.ANSWERING) return@withLock
            if (current.currentAnswererId != playerId) return@withLock

            timerJob?.cancel()
            Log.d(tag, "Player $playerId surrendered (لا أعلم). Transferring answer opportunity.")

            val newDisqualified = current.disqualifiedIds + playerId
            val remaining = current.players.filter {
                it.id != current.currentQuestionerId && it.id !in newDisqualified && it.isConnected
            }

            if (remaining.isNotEmpty()) {
                val next = remaining.random()
                Log.d(tag, "Transferred answer permission to: ${next.name}")
                _state.value = current.copy(
                    phase = GamePhase.ANSWERING,
                    currentAnswererId = next.id,
                    disqualifiedIds = newDisqualified,
                    remainingSeconds = current.duration.totalSeconds,
                    stateVersion = current.stateVersion + 1
                )
                startAnswerCountdown(current.duration.totalSeconds)
            } else {
                handleAllCompetitorsFailed(current.copy(disqualifiedIds = newDisqualified))
            }
        }
    }

    suspend fun proceedFromCards() {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.CARD_SELECTION) return@withLock
            _state.value = current.copy(
                phase = GamePhase.QUESTION_SETUP,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Proceeding from cards to QUESTION_SETUP.")
        }
    }

    suspend fun startQuestion(difficulty: QuestionDifficulty, duration: QuestionDuration) {
        mutex.withLock {
            val current = _state.value
            if (!duration.isAllowedFor(difficulty)) {
                Log.e(tag, "Disallowed duration: $duration for difficulty: $difficulty")
                return@withLock
            }

            timerJob?.cancel()
            fastestBuzzerArrivalNanos = Long.MAX_VALUE

            // The answerer was selected by the cards system!
            val answererId = current.currentAnswererId ?: current.cardPlayerIds.firstOrNull() ?: current.players.find { it.id != current.currentQuestionerId }?.id
            val answerer = current.players.find { it.id == answererId } ?: current.players.last()

            _state.value = current.copy(
                phase = GamePhase.ANSWERING,
                difficulty = difficulty,
                duration = duration,
                remainingSeconds = duration.totalSeconds,
                buzzerWinnerId = answerer.id,
                currentAnswererId = answerer.id,
                disqualifiedIds = emptySet(),
                lastRoundWinnerName = null,
                lastRoundPoints = 0,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Question started. First answerer is: ${answerer.name} for ${duration.totalSeconds}s.")
            startAnswerCountdown(duration.totalSeconds)
        }
    }

    private fun getElapsedRealtimeNanos(): Long {
        return try {
            SystemClock.elapsedRealtimeNanos()
        } catch (_: Throwable) {
            System.nanoTime()
        }
    }

    suspend fun onBuzzerPressed(playerId: String): Boolean {
        val arrivalNanos = getElapsedRealtimeNanos()
        return mutex.withLock {
            val current = _state.value

            if (current.phase != GamePhase.BUZZER_OPEN) {
                return@withLock false
            }

            if (playerId == current.currentQuestionerId) {
                return@withLock false
            }

            if (playerId in current.disqualifiedIds) {
                return@withLock false
            }

            val player = current.players.find { it.id == playerId && it.isConnected } ?: return@withLock false

            fastestBuzzerArrivalNanos = arrivalNanos
            _state.value = current.copy(
                phase = GamePhase.ANSWERING,
                buzzerWinnerId = player.id,
                currentAnswererId = player.id,
                remainingSeconds = current.duration.totalSeconds,
                stateVersion = current.stateVersion + 1
            )

            Log.d(tag, "Buzzer race WON by: ${player.name} at arrival timestamp: $arrivalNanos")
            startAnswerCountdown(current.duration.totalSeconds)
            true
        }
    }

    private fun startAnswerCountdown(seconds: Int) {
        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            var currentSec = seconds
            while (isActive && currentSec > 0) {
                delay(1000)
                currentSec--
                mutex.withLock {
                    if (_state.value.phase == GamePhase.ANSWERING) {
                        _state.value = _state.value.copy(
                            remainingSeconds = currentSec,
                            stateVersion = _state.value.stateVersion + 1
                        )
                    }
                }
            }

            if (currentSec == 0) {
                onAnswerTimeout()
            }
        }
    }

    private suspend fun onAnswerTimeout() {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.ANSWERING) return@withLock
            val answererId = current.currentAnswererId ?: return@withLock

            Log.d(tag, "Answer timer timed out for answerer: $answererId")
            val newDisqualified = current.disqualifiedIds + answererId
            val remaining = current.players.filter {
                it.id != current.currentQuestionerId && it.id !in newDisqualified && it.isConnected
            }

            if (remaining.isNotEmpty()) {
                val next = remaining.random()
                Log.d(tag, "Transferred answer permission to: ${next.name}")
                _state.value = current.copy(
                    phase = GamePhase.ANSWERING,
                    currentAnswererId = next.id,
                    disqualifiedIds = newDisqualified,
                    remainingSeconds = current.duration.totalSeconds,
                    stateVersion = current.stateVersion + 1
                )
                startAnswerCountdown(current.duration.totalSeconds)
            } else {
                handleAllCompetitorsFailed(current.copy(disqualifiedIds = newDisqualified))
            }
        }
    }

    suspend fun onAnswerDone(playerId: String) {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.ANSWERING) return@withLock
            if (current.currentAnswererId != playerId) return@withLock

            timerJob?.cancel()
            _state.value = current.copy(
                phase = GamePhase.JUDGING,
                stateVersion = current.stateVersion + 1
            )
            Log.d(tag, "Player $playerId submitted answer. Waiting for questioner judgment.")
        }
    }

    suspend fun onJudgeVerdict(isCorrect: Boolean) {
        mutex.withLock {
            val current = _state.value
            if (current.phase != GamePhase.JUDGING) return@withLock
            val answererId = current.currentAnswererId ?: return@withLock
            val answerer = current.players.find { it.id == answererId } ?: return@withLock

            if (isCorrect) {
                val pts = current.difficulty.points
                val updatedPlayers = current.players.map {
                    if (it.id == answererId) it.copy(score = it.score + pts) else it
                }
                _state.value = current.copy(
                    phase = GamePhase.ROUND_SUCCESS,
                    players = updatedPlayers,
                    lastRoundWinnerName = answerer.name,
                    lastRoundPoints = pts,
                    lastRoundSummary = "أجاب ${answerer.name} إجابة صحيحة وحصل على $pts نقطة!",
                    stateVersion = current.stateVersion + 1
                )
                Log.d(tag, "Judge: Correct! ${answerer.name} gained $pts pts.")
            } else {
                Log.d(tag, "Judge: Wrong answer from ${answerer.name}.")
                val newDisqualified = current.disqualifiedIds + answererId
                val remaining = current.players.filter {
                    it.id != current.currentQuestionerId && it.id !in newDisqualified && it.isConnected
                }
                if (remaining.isNotEmpty()) {
                    val next = remaining.random()
                    Log.d(tag, "Transferred answer permission to: ${next.name}")
                    _state.value = current.copy(
                        phase = GamePhase.ANSWERING,
                        currentAnswererId = next.id,
                        disqualifiedIds = newDisqualified,
                        remainingSeconds = current.duration.totalSeconds,
                        stateVersion = current.stateVersion + 1
                    )
                    startAnswerCountdown(current.duration.totalSeconds)
                } else {
                    handleAllCompetitorsFailed(current.copy(disqualifiedIds = newDisqualified))
                }
            }
        }
    }

    private fun handleAllCompetitorsFailed(baseState: GameState) {
        val questioner = baseState.players.find { it.id == baseState.currentQuestionerId }
        val pts = baseState.difficulty.points
        val updatedPlayers = baseState.players.map {
            if (it.id == baseState.currentQuestionerId) it.copy(score = it.score + pts) else it
        }
        val qName = questioner?.name ?: "السائل"
        _state.value = baseState.copy(
            phase = GamePhase.ROUND_FAILED,
            players = updatedPlayers,
            lastRoundWinnerName = qName,
            lastRoundPoints = pts,
            lastRoundSummary = "فشل المنافسون! النقاط تذهب للسائل ($qName) بواقع $pts نقطة.",
            stateVersion = baseState.stateVersion + 1
        )
        Log.d(tag, "All competitors failed. Points ($pts) awarded to questioner: $qName")
    }

    suspend fun resetSession() {
        mutex.withLock {
            timerJob?.cancel()
            val current = _state.value
            _state.value = current.copy(
                phase = if (current.players.size >= current.gameMode.maxPlayers) GamePhase.READY else GamePhase.WAITING_FOR_PLAYERS,
                currentAnswererId = null,
                buzzerWinnerId = null,
                disqualifiedIds = emptySet(),
                stateVersion = current.stateVersion + 1
            )
        }
    }
}
