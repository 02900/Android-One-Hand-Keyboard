package com.onehandkeyboard

import android.view.View
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for OneHandKeyboardService
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OneHandKeyboardServiceTest {

    private lateinit var service: OneHandKeyboardService

    @Before
    fun setup() {
        service = OneHandKeyboardService()
    }

    @Test
    fun `onCreateInputView returns non-null view`() {
        // Given
        // Service is initialized

        // When
        val inputView = service.onCreateInputView()

        // Then
        assertNotNull("Input view should not be null", inputView)
    }

    @Test
    fun `onCreateInputView returns ComposeView`() {
        // Given
        // Service is initialized

        // When
        val inputView = service.onCreateInputView()

        // Then
        assert(inputView is androidx.compose.ui.platform.ComposeView) {
            "Input view should be a ComposeView"
        }
    }
}
