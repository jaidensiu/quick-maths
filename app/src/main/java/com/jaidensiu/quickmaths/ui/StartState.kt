package com.jaidensiu.quickmaths.ui

enum class ModelStatus {
    LOADING,
    OFFLINE,
    READY,
    ERROR,
}

data class StartState(
    val modelStatus: ModelStatus = ModelStatus.LOADING,
    val bestTimeMs: Long? = null,
)
