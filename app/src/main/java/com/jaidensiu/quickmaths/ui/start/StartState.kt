package com.jaidensiu.quickmaths.ui.start

enum class ModelStatus {
    LOADING,
    OFFLINE,
    READY,
    ERROR,
}

data class StartState(
    val modelStatus: ModelStatus = ModelStatus.LOADING,
    val bestTimeMs: Long? = null,
) {
    val canStart: Boolean get() = modelStatus == ModelStatus.READY
}
