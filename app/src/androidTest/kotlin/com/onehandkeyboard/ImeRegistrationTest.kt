package com.onehandkeyboard

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test on a real device/emulator: the system must see the keyboard as an installed
 * input method (manifest service + android.view.im metadata are valid).
 */
@RunWith(AndroidJUnit4::class)
class ImeRegistrationTest {
    @Test
    fun keyboardIsRegisteredAsInputMethod() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val ours = imm.inputMethodList.filter { it.packageName == context.packageName }

        assertTrue("OneHandKeyboard is not registered as an IME", ours.isNotEmpty())
        assertTrue(ours.all { it.serviceName == OneHandKeyboardService::class.java.name })
    }
}
