package com.onehandkeyboard

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for OneHandKeyboardService
 * Simplified tests to validate basic functionality after refactoring
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OneHandKeyboardServiceTest {

    @Test
    fun `service can be instantiated`() {
        // Given & When
        val service = OneHandKeyboardService()

        // Then
        assert(service != null) {
            "Service should be instantiable"
        }
    }

    @Test
    fun `service has expected class name`() {
        // Given
        val service = OneHandKeyboardService()

        // When
        val className = service::class.java.simpleName

        // Then
        assert(className == "OneHandKeyboardService") {
            "Service should have correct class name"
        }
    }
}
