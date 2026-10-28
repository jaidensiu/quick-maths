package com.jaidensiu.quickmaths.navigation

import kotlinx.serialization.Serializable

sealed class AppRoute {
    @Serializable
    data object Start : AppRoute()

    @Serializable
    data object Settings : AppRoute()

    @Serializable
    data object Countdown : AppRoute()

    @Serializable
    data object Game : AppRoute()

    @Serializable
    data class Results(val elapsedTimeMs: Long) : AppRoute()
}
