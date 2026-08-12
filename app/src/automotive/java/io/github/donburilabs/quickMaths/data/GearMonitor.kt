package io.github.donburilabs.quickMaths.data

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GearMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val _isParked = MutableStateFlow(value = false)
    val isParked: StateFlow<Boolean> = _isParked.asStateFlow()

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            _isParked.value = value.value == VehicleGear.GEAR_PARK
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "Gear property error (propertyId=$propertyId, areaId=$areaId); blocking gameplay")
            _isParked.value = false
        }
    }

    init {
        Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { car, ready ->
            if (ready) {
                runCatching {
                    val manager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                    manager.registerCallback(
                        callback,
                        VehiclePropertyIds.GEAR_SELECTION,
                        CarPropertyManager.SENSOR_RATE_ONCHANGE,
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Unable to observe gear selection; blocking gameplay", error)
                    _isParked.value = false
                }
            } else {
                Log.w(TAG, "Car service disconnected; blocking gameplay")
                _isParked.value = false
            }
        }
    }

    private companion object {
        const val TAG = "GearMonitor"
    }
}
