package com.onehandkeyboard.ui

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.onehandkeyboard.prefs.KeyboardPreferences

/**
 * Immediate haptic and sound feedback. Both are driven by [KeyboardPreferences] and respect
 * the system: haptics through `performHapticFeedback`, sound is muted in silent/vibrate mode.
 */
class KeyFeedback(
    context: Context,
    private val prefs: KeyboardPreferences,
) {
    enum class Sound { STANDARD, DELETE, SPACE, RETURN }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** Finger lands on a key. */
    fun keyDown(
        view: View,
        sound: Sound,
    ) {
        if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (prefs.soundEnabled) playSound(sound)
    }

    /** Swipe crosses the threshold or changes direction. */
    fun directionTick(view: View) {
        if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun playSound(sound: Sound) {
        val audio = audioManager ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val effect =
            when (sound) {
                Sound.STANDARD -> AudioManager.FX_KEYPRESS_STANDARD
                Sound.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                Sound.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                Sound.RETURN -> AudioManager.FX_KEYPRESS_RETURN
            }
        audio.playSoundEffect(effect, SOUND_VOLUME)
    }

    private companion object {
        const val SOUND_VOLUME = -1f // system default key-click volume
    }
}
