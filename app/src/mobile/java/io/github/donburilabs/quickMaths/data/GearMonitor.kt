package io.github.donburilabs.quickMaths.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Mobile devices have no gear selector; gameplay is always allowed. */
@Singleton
class GearMonitor @Inject constructor() {
    val isParked: StateFlow<Boolean> = MutableStateFlow(value = true).asStateFlow()
}
