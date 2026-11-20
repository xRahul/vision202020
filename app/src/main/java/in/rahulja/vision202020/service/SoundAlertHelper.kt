package `in`.rahulja.vision202020.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundAlertHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            // Audio device unavailable or restricted
            Log.d("SoundAlertHelper", "ToneGenerator init failed", e)
        }
    }

    fun playBreakStarted(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP, 250)
        }
        if (vibrationEnabled) {
            vibrate(longArrayOf(0, 300, 200, 300))
        }
    }

    fun playBreakEnded(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (soundEnabled) {
            playTone(ToneGenerator.TONE_PROP_ACK, 200)
        }
        if (vibrationEnabled) {
            vibrate(longArrayOf(0, 150, 100, 150))
        }
    }

    private fun playTone(tone: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(tone, durationMs)
        } catch (e: Exception) {
            // Ignore audio failure
            Log.d("SoundAlertHelper", "ToneGenerator start failed", e)
        }
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (e: Exception) {
            // Ignore vibration failure
            Log.d("SoundAlertHelper", "Vibration failed", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
            Log.d("SoundAlertHelper", "ToneGenerator release failed", e)
        }
    }
}
