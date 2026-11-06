package com.example

import com.example.engine.GameEngine
import com.example.model.GamePhase
import com.example.model.QuestionDifficulty
import com.example.model.QuestionDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals(GamePhase.BUZZER_OPEN, engine.state.value.phase)
    }

    @Test
    fun `test atomic buzzer race selects exactly one winner`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true) // questioner
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.EASY, QuestionDuration.ONE_MINUTE)
        assertEquals(GamePhase.BUZZER_OPEN, engine.state.value.phase)

        // Questioner cannot buzz
        val qBuzz = engine.onBuzzerPressed("p1")
        assertFalse(qBuzz)

        // First competitor buzzes and wins
        val buzz1 = engine.onBuzzerPressed("p2")
        assertTrue(buzz1)
        assertEquals(GamePhase.ANSWERING, engine.state.value.phase)
        assertEquals("p2", engine.state.value.buzzerWinnerId)
        assertEquals("p2", engine.state.value.currentAnswererId)

        // Subsequent competitor buzzes are rejected
        val buzz2 = engine.onBuzzerPressed("p3")
        assertFalse(buzz2)
        assertEquals("p2", engine.state.value.currentAnswererId)
    }

    @Test
    fun `test correct answer awards points to answerer and ends round`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true)
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.HARD, QuestionDuration.TWO_MINUTES)
        engine.onBuzzerPressed("p2")
        engine.onAnswerDone("p2")

        assertEquals(GamePhase.JUDGING, engine.state.value.phase)

        engine.onJudgeVerdict(isCorrect = true)

        assertEquals(GamePhase.ROUND_SUCCESS, engine.state.value.phase)
        val p2 = engine.state.value.players.find { it.id == "p2" }
        assertEquals(3, p2?.score) // Hard = 3 points
        assertEquals("عمر", engine.state.value.lastRoundWinnerName)
    }

    @Test
    fun `test wrong answer disqualifies competitor and transfers to next random remaining`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true) // questioner
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.MEDIUM, QuestionDuration.TWO_MINUTES)
        engine.onBuzzerPressed("p2")
        engine.onAnswerDone("p2")

        // First competitor answers wrong
        engine.onJudgeVerdict(isCorrect = false)

        assertEquals(GamePhase.ANSWERING, engine.state.value.phase)
        assertTrue(engine.state.value.disqualifiedIds.contains("p2"))
        val nextAnswererId = engine.state.value.currentAnswererId
        assertTrue(nextAnswererId == "p3" || nextAnswererId == "p4")
    }

    @Test
    fun `test all competitors failing awards points to questioner`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true) // questioner
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startQuestion(QuestionDifficulty.MEDIUM, QuestionDuration.TWO_MINUTES)

        // 1st competitor fails
        engine.onBuzzerPressed("p2")
        engine.onAnswerDone("p2")
        engine.onJudgeVerdict(isCorrect = false)

        val secondId = engine.state.value.currentAnswererId!!
        engine.onAnswerDone(secondId)
        engine.onJudgeVerdict(isCorrect = false)

        val thirdId = engine.state.value.currentAnswererId!!
        engine.onAnswerDone(thirdId)
        engine.onJudgeVerdict(isCorrect = false)

        // All 3 failed! Questioner should receive the points
        assertEquals(GamePhase.ROUND_FAILED, engine.state.value.phase)
        val questioner = engine.state.value.players.find { it.id == "p1" }
        assertEquals(2, questioner?.score) // Medium = 2 points
        assertEquals("خالد", engine.state.value.lastRoundWinnerName)
    }

    @Test
    fun `test wheel spinning selects a player among all four`() = runTest(testDispatcher) {
        engine.addPlayer("p1", "خالد", true)
        engine.addPlayer("p2", "عمر", false)
        engine.addPlayer("p3", "سارة", false)
        engine.addPlayer("p4", "فاطمة", false)

        engine.startWheelSpin()
        assertEquals(GamePhase.SPINNING_WHEEL, engine.state.value.phase)
        val targetId = engine.state.value.wheelTargetPlayerId
        assertNotNull(targetId)
        assertTrue(engine.state.value.players.any { it.id == targetId })

        engine.completeWheelSpin()
        assertEquals(GamePhase.QUESTION_SETUP, engine.state.value.phase)
        assertEquals(targetId, engine.state.value.currentQuestionerId)
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

    @Test
    fun `test duo mode with two players and alternating turns`() = runTest(testDispatcher) {
        val duoEngine = GameEngine(testScope, "2222", "192.168.1.10", com.example.model.GameMode.DUO)

        val p1 = duoEngine.addPlayer("p1", "خالد", isHost = true)
        assertTrue(p1.first)
        assertEquals(1, duoEngine.state.value.players.size)
        assertEquals(GamePhase.WAITING_FOR_PLAYERS, duoEngine.state.value.phase)

        val p2 = duoEngine.addPlayer("p2", "عمر", isHost = false)
        assertTrue(p2.first)
        assertEquals(2, duoEngine.state.value.players.size)
        assertEquals(GamePhase.READY, duoEngine.state.value.phase)

        // 3rd player rejected in Duo mode
        val p3 = duoEngine.addPlayer("p3", "سارة", isHost = false)
        assertFalse(p3.first)

        // Question 1: p1 is questioner. Start question
        duoEngine.startQuestion(QuestionDifficulty.EASY, QuestionDuration.ONE_MINUTE)
        // In Duo mode: direct answerer assignment without buzzer race!
        assertEquals(GamePhase.ANSWERING, duoEngine.state.value.phase)
        assertEquals("p2", duoEngine.state.value.currentAnswererId)

        // p2 answers correct
        duoEngine.onAnswerDone("p2")
        duoEngine.onJudgeVerdict(isCorrect = true)
        assertEquals(GamePhase.ROUND_SUCCESS, duoEngine.state.value.phase)
        assertEquals(1, duoEngine.state.value.players.find { it.id == "p2" }?.score)

        // Next round: strictly alternate turns ("كل شخص مرة يسأل ثم مرة يجاوب")
        duoEngine.startWheelSpin()
        assertEquals(GamePhase.SPINNING_WHEEL, duoEngine.state.value.phase)
        assertEquals("p2", duoEngine.state.value.wheelTargetPlayerId)

        duoEngine.completeWheelSpin()
        assertEquals(GamePhase.QUESTION_SETUP, duoEngine.state.value.phase)
        assertEquals("p2", duoEngine.state.value.currentQuestionerId)

        // Question 2: Now p2 is questioner, p1 will be answerer!
        duoEngine.startQuestion(QuestionDifficulty.MEDIUM, QuestionDuration.TWO_MINUTES)
        assertEquals(GamePhase.ANSWERING, duoEngine.state.value.phase)
        assertEquals("p1", duoEngine.state.value.currentAnswererId)

        // p1 answers wrong -> points to p2 (questioner)
        duoEngine.onAnswerDone("p1")
        duoEngine.onJudgeVerdict(isCorrect = false)
        assertEquals(GamePhase.ROUND_FAILED, duoEngine.state.value.phase)
        // p2 should get 2 points: original 1 + 2 = 3
        assertEquals(3, duoEngine.state.value.players.find { it.id == "p2" }?.score)
    }

    @Test
    fun `test trio mode with three players and two competitors on buzzer`() = runTest(testDispatcher) {
        val trioEngine = GameEngine(testScope, "3333", "192.168.1.10", com.example.model.GameMode.TRIO)

        trioEngine.addPlayer("p1", "خالد", true)
        trioEngine.addPlayer("p2", "عمر", false)
        trioEngine.addPlayer("p3", "سارة", false)
        assertEquals(3, trioEngine.state.value.players.size)
        assertEquals(GamePhase.READY, trioEngine.state.value.phase)

        // 4th player rejected in Trio mode
        val p4 = trioEngine.addPlayer("p4", "فاطمة", false)
        assertFalse(p4.first)

        // Start question -> BUZZER_OPEN for the 2 competitors
        trioEngine.startQuestion(QuestionDifficulty.HARD, QuestionDuration.TWO_MINUTES)
        assertEquals(GamePhase.BUZZER_OPEN, trioEngine.state.value.phase)

        // p2 buzzes
        val buzzed = trioEngine.onBuzzerPressed("p2")
        assertTrue(buzzed)
        assertEquals(GamePhase.ANSWERING, trioEngine.state.value.phase)
        assertEquals("p2", trioEngine.state.value.currentAnswererId)

        // p2 answers wrong -> transferred to p3
        trioEngine.onAnswerDone("p2")
        trioEngine.onJudgeVerdict(isCorrect = false)
        assertEquals(GamePhase.ANSWERING, trioEngine.state.value.phase)
        assertEquals("p3", trioEngine.state.value.currentAnswererId)
    }
}
