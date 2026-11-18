package com.onehandkeyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputConnection
import androidx.core.content.ContextCompat

/**
 * Voice typing through Android's speech recognition service (the one Gboard uses).
 *
 * - On-device recognition is preferred (Android 12+, when the language is downloaded); if it
 *   is not available the system's recognition service is used, which may send the audio to
 *   its provider. The keyboard itself never sends anything.
 * - While the user speaks, the partial text is shown as composing text; the final result is
 *   committed. Any key, or a tap on the strip, ends the dictation.
 * - Needs the microphone permission, which the keyboard cannot ask for: [onNeedsPermission]
 *   opens the settings screen that asks.
 *
 * Main thread only (SpeechRecognizer requirement). Nothing heard is logged.
 */
class VoiceTyping(
    private val context: Context,
    private val connection: () -> InputConnection?,
    /** BCP-47 tag of the language to recognise (e.g. "es-AR"). */
    private val languageTag: () -> String,
) {
    /** Status line for the suggestion strip, or null when not listening. */
    var onStatus: (String?) -> Unit = {}
    var onNeedsPermission: () -> Unit = {}
    var onMessage: (String) -> Unit = {}

    private var recognizer: SpeechRecognizer? = null
    private var onDevice = false
    private var prefix = ""
    private var capitalize = false
    private var heard = false

    val isListening: Boolean get() = recognizer != null

    fun toggle() = if (isListening) stop() else start()

    fun start() {
        if (isListening) return
        if (!hasPermission()) {
            onNeedsPermission()
            return
        }
        val before = connection()?.getTextBeforeCursor(CONTEXT_CHARS, 0)
        prefix = VoiceText.prefix(before)
        capitalize = VoiceText.startsSentence(before)
        listen(preferOnDevice = true)
    }

    /** Ends the dictation; the recognizer still delivers what it heard so far. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Drops the dictation (keyboard hidden), keeping the text already shown. */
    fun cancel() {
        if (!isListening) return
        connection()?.finishComposingText()
        end()
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun listen(preferOnDevice: Boolean) {
        val created = create(preferOnDevice)
        if (created == null) {
            onMessage("Este teléfono no tiene reconocimiento de voz")
            return
        }
        recognizer = created
        heard = false
        created.setRecognitionListener(listener)
        onStatus(STATUS_STARTING)
        created.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName),
        )
    }

    private fun create(preferOnDevice: Boolean): SpeechRecognizer? {
        val canOnDevice =
            preferOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        onDevice = canOnDevice
        return when {
            canOnDevice -> SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            SpeechRecognizer.isRecognitionAvailable(context) -> SpeechRecognizer.createSpeechRecognizer(context)
            else -> null
        }
    }

    private fun end() {
        recognizer?.destroy()
        recognizer = null
        onStatus(null)
    }

    private fun show(
        results: Bundle?,
        final: Boolean,
    ) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        val ic = connection() ?: return
        if (text.isBlank()) {
            if (final) ic.finishComposingText()
            return
        }
        heard = true
        val formatted = prefix + VoiceText.format(text, capitalize)
        if (final) ic.commitText(formatted, 1) else ic.setComposingText(formatted, 1)
    }

    private val listener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onStatus(STATUS_LISTENING)

            override fun onPartialResults(partialResults: Bundle?) = show(partialResults, final = false)

            override fun onResults(results: Bundle?) {
                show(results, final = true)
                end()
            }

            override fun onError(error: Int) {
                connection()?.finishComposingText()
                end()
                when {
                    // The on-device model is missing for this language: try the regular service.
                    onDevice && !heard && error in LANGUAGE_ERRORS -> listen(preferOnDevice = false)
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> onNeedsPermission()
                    error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        if (!heard) onMessage("No se escuchó nada")
                    error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        onMessage("El dictado necesita conexión")
                    else -> onMessage("No se pudo usar el dictado")
                }
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onEvent(
                eventType: Int,
                params: Bundle?,
            ) = Unit
        }

    companion object {
        private const val CONTEXT_CHARS = 8
        const val STATUS_STARTING = "🎤 …"
        const val STATUS_LISTENING = "🎤 Escuchando… toca aquí para terminar"

        // SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED / ERROR_LANGUAGE_UNAVAILABLE (API 31).
        private val LANGUAGE_ERRORS = setOf(12, 13)
    }
}

/** Spacing and capitals of dictated text, from the text before the cursor. */
object VoiceText {
    private val SENTENCE_END = setOf('.', '!', '?', '¡', '¿', '\n')

    /** A space when the dictation would otherwise stick to the previous word. */
    fun prefix(before: CharSequence?): String =
        if (before.isNullOrEmpty() || before.last().isWhitespace() || before.last() in "¿¡(\"") "" else " "

    /** True at the start of the field or after a sentence end. */
    fun startsSentence(before: CharSequence?): Boolean {
        val trimmed = before?.trimEnd() ?: ""
        return trimmed.isEmpty() || trimmed.last() in SENTENCE_END
    }

    /** Capitalizes the first letter when a sentence starts; never lowercases (names stay). */
    fun format(
        text: String,
        capitalize: Boolean,
    ): String = if (capitalize) text.replaceFirstChar { it.titlecase() } else text
}
