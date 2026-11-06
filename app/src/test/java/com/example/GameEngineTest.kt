package com.example

import com.example.engine.GameEngine
import com.example.model.GamePhase
import com.example.model.QuestionDifficulty
import com.example.model.QuestionDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var engine: GameEngine

    @Before
    fun setup() {
        engine = GameEngine(testScope, "5555", "192.168.1.10")
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `test adding four players and rejecting fifth player`() = runTest(testDispatcher) {
        // Player 1 (Host)
        val p1 = engine.addPlayer("p1", "خالد", isHost = true)
        assertTrue(p1.first)
        assertEquals(1, engine.state.value.players.size)
        assertEquals(GamePhase.WAITING_FOR_PLAYERS, engine.state.value.phase)

        // Player 2
        val p2 = engine.addPlayer("p2", "عمر", isHost = false)
        assertTrue(p2.first)

        // Player 3
        val p3 = engine.addPlayer("p3", "سارة", isHost = false)
        assertTrue(p3.first)

        // Player 4 (Completes room)
        val p4 = engine.addPlayer("p4", "فاطمة", isHost = false)
        assertTrue(p4.first)
        assertEquals(4, engine.state.value.players.size)
        assertEquals(GamePhase.READY, engine.state.value.phase)

        // 5th player rejected
        val p5 = engine.addPlayer("p5", "زيد", isHost = false)
        assertFalse(p5.first)
        assertNotNull(p5.second)
        assertEquals(4, engine.state.value.players.size)
    }

    @Test
    fun `test hard difficulty forbids one minute duration`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true)
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        assertFalse(QuestionDuration.ONE_MINUTE.isAllowedFor(QuestionDifficulty.HARD))
        assertTrue(QuestionDuration.TWO_MINUTES.isAllowedFor(QuestionDifficulty.HARD))
        assertTrue(QuestionDuration.THREE_MINUTES.isAllowedFor(QuestionDifficulty.HARD))
        assertTrue(QuestionDuration.FOUR_MINUTES.isAllowedFor(QuestionDifficulty.HARD))

        // Attempting to start with 1 min on Hard is rejected by server engine
        engine.startQuestion(QuestionDifficulty.HARD, QuestionDuration.ONE_MINUTE)
        assertEquals(GamePhase.READY, engine.state.value.phase)

        // Allowed duration works
        engine.startQuestion(QuestionDifficulty.HARD, QuestionDuration.TWO_MINUTES)
        assertEquals(GamePhase.ANSWERING, engine.state.value.phase)
    }

    @Test
    fun `test correct answer awards points to answerer and ends round`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true)
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.HARD, QuestionDuration.TWO_MINUTES)
        val answererId = engine.state.value.currentAnswererId!!
        val answerer = engine.state.value.players.find { it.id == answererId }!!

        engine.onAnswerDone(answererId)
        assertEquals(GamePhase.JUDGING, engine.state.value.phase)

        engine.onJudgeVerdict(isCorrect = true)

        assertEquals(GamePhase.ROUND_SUCCESS, engine.state.value.phase)
        val updated = engine.state.value.players.find { it.id == answererId }
        assertEquals(3, updated?.score) // Hard = 3 points
        assertEquals(answerer.name, engine.state.value.lastRoundWinnerName)
    }

    @Test
    fun `test wrong answer disqualifies competitor and transfers to next random remaining`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true) // questioner
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.MEDIUM, QuestionDuration.TWO_MINUTES)
        val firstId = engine.state.value.currentAnswererId!!
        engine.onAnswerDone(firstId)

        // First competitor answers wrong
        engine.onJudgeVerdict(isCorrect = false)

        assertEquals(GamePhase.ANSWERING, engine.state.value.phase)
        assertTrue(engine.state.value.disqualifiedIds.contains(firstId))
        val nextAnswererId = engine.state.value.currentAnswererId
        assertNotNull(nextAnswererId)
        assertFalse(nextAnswererId == firstId)
        assertFalse(nextAnswererId == "p1") // Not questioner
    }

    @Test
    fun `test all competitors failing awards points to questioner`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true) // questioner
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.MEDIUM, QuestionDuration.TWO_MINUTES)

        // 1st competitor fails
        val firstId = engine.state.value.currentAnswererId!!
        engine.onAnswerDone(firstId)
        engine.onJudgeVerdict(isCorrect = false)

        val secondId = engine.state.value.currentAnswererId!!
        engine.onAnswerDone(secondId)
        engine.onJudgeVerdict(isCorrect = false)

        val thirdId = engine.state.value.currentAnswererId!!
        engine.onAnswerDone(thirdId)
        engine.onJudgeVerdict(isCorrect = false)

        // All 3 competitors failed! Questioner receives the points
        assertEquals(GamePhase.ROUND_FAILED, engine.state.value.phase)
        val questioner = engine.state.value.players.find { it.id == "p1" }
        assertEquals(2, questioner?.score) // Medium = 2 points
        assertEquals("خالد", engine.state.value.lastRoundWinnerName)
    }

    @Test
    fun `test wheel spinning and card selection flow`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true)
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startWheelSpin()
        assertEquals(GamePhase.SPINNING_WHEEL, engine.state.value.phase)
        val targetId = engine.state.value.wheelTargetPlayerId
        assertNotNull(targetId)
        assertTrue(engine.state.value.players.any { it.id == targetId })

        // Complete wheel spin -> enters CARD_SELECTION with candidate cards
        engine.completeWheelSpin()
        assertEquals(GamePhase.CARD_SELECTION, engine.state.value.phase)
        assertEquals(targetId, engine.state.value.currentQuestionerId)
        assertEquals(3, engine.state.value.cardPlayerIds.size)
        assertFalse(engine.state.value.cardPlayerIds.contains(targetId)) // Questioner is not in candidates

        // Cards flipped to back
        engine.onCardsFlippedToBack()
        assertTrue(engine.state.value.cardsFlippedToBack)

        // Questioner selects card at index 1
        engine.onCardSelected(1)
        assertEquals(1, engine.state.value.selectedCardIndex)
        val chosenAnswererId = engine.state.value.selectedCardPlayerId
        assertNotNull(chosenAnswererId)
        assertEquals(chosenAnswererId, engine.state.value.currentAnswererId)

        // Proceed to question setup
        engine.proceedFromCards()
        assertEquals(GamePhase.QUESTION_SETUP, engine.state.value.phase)
    }

    @Test
    fun `test player disconnection and reconnection`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true)
        engine.addPlayer("p2", "عمر", false)

        engine.onPlayerDisconnected("p2")
        val p2Disc = engine.state.value.players.find { it.id == "p2" }
        assertFalse(p2Disc!!.isConnected)

        // Reconnect with same ID
        engine.addPlayer("p2", "عمر", false)
        val p2Rec = engine.state.value.players.find { it.id == "p2" }
        assertTrue(p2Rec!!.isConnected)
    }
}
