package com.onehandkeyboard.core.mapping.di

import com.onehandkeyboard.core.mapping.DefaultMappingService
import com.onehandkeyboard.core.mapping.MappingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for mapping dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MappingModule {
    
    @Binds
    @Singleton
    abstract fun bindMappingService(
        defaultMappingService: DefaultMappingService
    ): MappingService
}
