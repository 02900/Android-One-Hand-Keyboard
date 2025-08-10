package com.onehandkeyboard.core.gesture.di

import com.onehandkeyboard.core.gesture.DefaultGestureDetector
import com.onehandkeyboard.core.gesture.GestureDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for gesture detection dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GestureModule {
    
    @Binds
    @Singleton
    abstract fun bindGestureDetector(
        defaultGestureDetector: DefaultGestureDetector
    ): GestureDetector
}
