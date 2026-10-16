package com.jaidensiu.quickmaths.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Mobile devices have no gear selector; gameplay is always allowed. */
@Singleton
class GearMonitor @Inject constructor() {
    /** `true` always; `null` (unknown) and `false` only occur on the automotive flavor. */
    val isParked: StateFlow<Boolean?> = MutableStateFlow<Boolean?>(value = true).asStateFlow()
}
