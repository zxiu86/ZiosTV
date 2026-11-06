package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FeedbackType {
    BUZZER_WINNER,
    BUZZER_LOSER,
    BUZZER_HOST_NOTIFY,
    ANSWER_TRANSFERRED,
    CORRECT_ANSWER,
    WRONG_ANSWER,
    WHEEL_TICK,
    WHEEL_SELECTED,
    CARD_FLIP,
    CARD_SELECTED
}

class FeedbackManager(private val context: Context) {

    private val tag = "FeedbackManager"
    private val feedbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private var toneGenerator: ToneGenerator? = null

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    init {
        initToneGenerator()
    }

    private fun initToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w(tag, "Could not initialize ToneGenerator: ${e.message}")
        }
    }

    fun playFeedback(type: FeedbackType, scope: CoroutineScope? = null) {
        val targetScope = scope ?: feedbackScope
        targetScope.launch(Dispatchers.Default) {
            val inSilentMode = audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL
            val shouldPlaySound = isSoundEnabled && !inSilentMode

            when (type) {
                FeedbackType.BUZZER_WINNER -> {
                    if (shouldPlaySound) {
                        playToneSequence(listOf(ToneGenerator.TONE_PROP_BEEP2 to 120, ToneGenerator.TONE_CDMA_CONFIRM to 180))
                    }
                    if (isVibrationEnabled) {
                        vibratePattern(longArrayOf(0, 80, 50, 120))
                    }
                }

                FeedbackType.BUZZER_LOSER -> {
                    if (isVibrationEnabled) {
                        vibrateOneShot(25)
                    }
                }

                FeedbackType.BUZZER_HOST_NOTIFY -> {
                    if (shouldPlaySound) {
                        playTone(ToneGenerator.TONE_PROP_PROMPT, 100)
                    }
                    if (isVibrationEnabled) {
                        vibrateOneShot(40)
                    }
                }

                FeedbackType.ANSWER_TRANSFERRED -> {
                    if (shouldPlaySound) {
                        playTone(ToneGenerator.TONE_PROP_BEEP, 140)
                    }
                    if (isVibrationEnabled) {
                        vibratePattern(longArrayOf(0, 100, 70, 100))
                    }
                }

                FeedbackType.CORRECT_ANSWER -> {
                    if (shouldPlaySound) {
                        playToneSequence(listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 100, ToneGenerator.TONE_PROP_ACK to 220))
                    }
                    if (isVibrationEnabled) {
                        vibratePattern(longArrayOf(0, 60, 40, 140))
                    }
                }

                FeedbackType.WRONG_ANSWER -> {
                    if (shouldPlaySound) {
                        playTone(ToneGenerator.TONE_PROP_NACK, 260)
                    }
                    if (isVibrationEnabled) {
                        vibratePattern(longArrayOf(0, 180))
                    }
                }

                FeedbackType.WHEEL_TICK -> {
                    if (shouldPlaySound) {
                        playTone(ToneGenerator.TONE_PROP_BEEP, 25)
                    }
                    if (isVibrationEnabled) {
                        vibrateOneShot(10)
                    }
                }

                FeedbackType.WHEEL_SELECTED -> {
                    if (shouldPlaySound) {
                        playToneSequence(listOf(ToneGenerator.TONE_PROP_BEEP2 to 100, ToneGenerator.TONE_CDMA_CONFIRM to 250))
                    }
                    if (isVibrationEnabled) {
                        vibratePattern(longArrayOf(0, 90, 60, 160))
                    }
                }

                FeedbackType.CARD_FLIP -> {
                    if (shouldPlaySound) {
                        playTone(ToneGenerator.TONE_PROP_BEEP, 40)
                    }
                    if (isVibrationEnabled) {
                        vibrateOneShot(18)
                    }
                }

                FeedbackType.CARD_SELECTED -> {
                    if (shouldPlaySound) {
                        playToneSequence(listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 90, ToneGenerator.TONE_CDMA_CONFIRM to 220))
                    }
                    if (isVibrationEnabled) {
                        vibratePattern(longArrayOf(0, 70, 40, 120))
                    }
                }
            }
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Exception) {}
    }

    private suspend fun playToneSequence(tones: List<Pair<Int, Int>>) {
        for ((tone, duration) in tones) {
            playTone(tone, duration)
            delay(duration.toLong() + 15)
        }
    }

    private fun vibrateOneShot(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        feedbackScope.cancel()
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
