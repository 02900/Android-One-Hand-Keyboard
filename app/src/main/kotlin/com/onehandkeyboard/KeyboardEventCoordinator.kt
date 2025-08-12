package com.onehandkeyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.widget.Button

/**
 * Coordinates keyboard events, gestures, and actions between components.
 * 
 * This component handles:
 * - Event coordination between UI, gestures, and actions
 * - Button click listener setup and management
 * - Gesture integration coordination
 * - Action processing coordination
 * - Accent options handling
 * - Keyboard recreation triggers
 * 
 * Extracted from OneHandKeyboardService.kt as part of SRP refactoring.
 */
class KeyboardEventCoordinator(
    private val service: InputMethodService,
    private val layoutManager: KeyboardLayoutManager,
    private val viewBuilder: KeyboardViewBuilder,
    private val gestureIntegration: GestureIntegration,
    private val actionProcessor: KeyboardActionProcessor
) {
    
    /**
     * Sets up all event listeners and gesture integration for the keyboard
     */
    fun setupKeyboardEvents(keyboardView: View) {
        Log.d("KeyboardEventCoordinator", "Setting up keyboard events and gesture integration")
        
        // Setup gesture integration with the main keyboard view
        gestureIntegration.setupGestureIntegration(keyboardView)
        
        // Setup control button listeners
        setupControlButtonListeners(keyboardView)
        
        Log.d("KeyboardEventCoordinator", "Keyboard events setup completed")
    }
    
    /**
     * Sets up click listeners for control buttons (Layout, Case mode)
     */
    private fun setupControlButtonListeners(keyboardView: View) {
        // Find and setup layout button
        findLayoutButton(keyboardView)?.setOnClickListener {
            Log.d("KeyboardEventCoordinator", "Layout button clicked")
            handleLayoutSwitch()
        }
        
        // Find and setup case button (if visible)
        findCaseButton(keyboardView)?.setOnClickListener {
            Log.d("KeyboardEventCoordinator", "Case button clicked")
            handleCaseSwitch()
        }
    }
    
    /**
     * Handles layout switching and keyboard recreation
     */
    private fun handleLayoutSwitch() {
        layoutManager.switchLayout()
        recreateKeyboard()
    }
    
    /**
     * Handles case mode switching and keyboard recreation
     */
    private fun handleCaseSwitch() {
        layoutManager.switchCaseMode()
        recreateKeyboard()
    }
    
    /**
     * Triggers keyboard recreation with current configuration
     */
    fun recreateKeyboard() {
        Log.d("KeyboardEventCoordinator", "Recreating keyboard with configuration: ${layoutManager.getCurrentConfigurationState()}")
        
        try {
            // Create new keyboard view
            val newKeyboardView = viewBuilder.createMainKeyboard()
            
            // Setup events for new view
            setupKeyboardEvents(newKeyboardView)
            
            // Update the service's input view
            service.setInputView(newKeyboardView)
            
            Log.d("KeyboardEventCoordinator", "Keyboard recreation completed successfully")
        } catch (e: Exception) {
            Log.e("KeyboardEventCoordinator", "Error recreating keyboard", e)
        }
    }
    
    /**
     * Handles accent options display (simple implementation)
     */
    fun showAccentOptions(accentChars: String) {
        Log.d("KeyboardEventCoordinator", "Showing accent options: $accentChars")
        
        // Simple implementation: send first accent character
        if (accentChars.isNotEmpty()) {
            val firstAccent = accentChars.first().toString()
            Log.d("KeyboardEventCoordinator", "Sending accent: $firstAccent")
            service.currentInputConnection?.commitText(firstAccent, 1)
        }
    }
    
    /**
     * Finds the layout button in the keyboard view hierarchy
     */
    private fun findLayoutButton(keyboardView: View): Button? {
        return findButtonByText(keyboardView, layoutManager.getCurrentLayoutModeText())
    }
    
    /**
     * Finds the case button in the keyboard view hierarchy
     */
    private fun findCaseButton(keyboardView: View): Button? {
        return if (layoutManager.isLettersMode()) {
            findButtonByText(keyboardView, layoutManager.getCurrentCaseModeText())
        } else null
    }
    
    /**
     * Recursively finds a button by its text content
     */
    private fun findButtonByText(view: View, text: String): Button? {
        if (view is Button && view.text.toString() == text) {
            return view
        }
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findButtonByText(view.getChildAt(i), text)
                if (found != null) return found
            }
        }
        
        return null
    }
    
    /**
     * Gets current gesture integration status for debugging
     */
    fun getGestureIntegrationStatus(): String {
        return "Gesture integration active: ${gestureIntegration.isInitialized()}"
    }
    
    /**
     * Gets current action processor status for debugging
     */
    fun getActionProcessorStatus(): String {
        return "Action processor ready: ${actionProcessor.isReady()}"
    }
    
    /**
     * Handles cleanup when keyboard is destroyed
     */
    fun cleanup() {
        Log.d("KeyboardEventCoordinator", "Cleaning up keyboard event coordinator")
        gestureIntegration.cleanup()
    }
}
