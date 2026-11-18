package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.actions.LabelContext
import com.onehandkeyboard.actions.label
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KeyIconsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val icons = KeyIcons(context)

    /** Labels of control actions: either readable text (123, ABC, ES) or an icon, never an emoji. */
    @Test
    fun `every control key label is text or has an icon`() {
        val labels = controlLabels() + enterLabels()
        val missing = labels.filter { label -> label.none { it.isLetterOrDigit() } && !icons.has(label) }
        assertTrue("labels without an icon: $missing", missing.isEmpty())
    }

    @Test
    fun `every icon loads and draws with the key color`() {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        KeyIcons.ICONS.values.toSet().forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
        assertTrue(icons.draw(Canvas(bitmap), "⌫", 24f, 24f, 40f, Color.RED))
        assertFalse(icons.draw(Canvas(bitmap), "a", 24f, 24f, 40f, Color.RED))
    }

    /** Labels of every non-typing action in every layout, for every case mode. */
    private fun controlLabels(): Set<String> {
        val factory = KeyboardConfigurationFactory()
        val configs =
            Language.entries.flatMap { language ->
                LayoutMode.entries.mapNotNull { mode ->
                    runCatching { factory.getConfiguration(language, mode) }.getOrNull()?.let { mode to it }
                }
            }
        return configs
            .flatMap { (mode, config) ->
                config.cells.flatMap { it.actions.values }.filter(::isControl).flatMap { action ->
                    CaseMode.entries.map { case -> action.label(LabelContext(caseMode = case, layoutMode = mode)) }
                }
            }.toSet()
    }

    private fun isControl(action: ActionConfig) =
        when (action) {
            is ActionConfig.Character, is ActionConfig.Text, is ActionConfig.CustomFunction -> false
            is ActionConfig.Action -> action.keyAction.let { it !is KeyAction.DeadKey && it !is KeyAction.InsertText }
            else -> true
        }

    private fun enterLabels() =
        listOf(
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_PREVIOUS,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_UNSPECIFIED,
        ).map { action ->
            GestureHintProvider.enterLabelFor(
                EditorInfo().apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    imeOptions = action
                },
            )
        }
}
