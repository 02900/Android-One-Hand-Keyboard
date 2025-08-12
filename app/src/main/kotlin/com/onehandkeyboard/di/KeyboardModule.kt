package com.onehandkeyboard.di

import com.onehandkeyboard.KeyboardConfigManager
import com.onehandkeyboard.KeyboardUiFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import android.inputmethodservice.InputMethodService

/**
 * Módulo de inyección de dependencias para componentes del teclado
 * 
 * Proporciona instancias configuradas de componentes del teclado.
 * Nota: KeyboardUiFactory no se puede inyectar directamente porque requiere
 * el contexto del servicio que no está disponible en el momento de la inyección.
 */
@Module
@InstallIn(ServiceComponent::class)
object KeyboardModule {
    
    // KeyboardConfigManager se inyecta automáticamente como @Singleton
    // KeyboardUiFactory se inicializa manualmente en el servicio debido a dependencias de contexto
}
