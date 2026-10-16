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
    /** `null` while the vehicle gear is unknown; always `true` on mobile. */
    val isParked: Boolean? = null,
) {
    val canStart: Boolean get() = modelStatus == ModelStatus.READY && isParked == true
}
