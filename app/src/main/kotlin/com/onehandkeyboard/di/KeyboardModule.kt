package com.onehandkeyboard.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

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
    // KeyboardLayoutManager se inyecta automáticamente como @Singleton
    // KeyboardUiFactory se inicializa manualmente en el servicio debido a dependencias de contexto
}
