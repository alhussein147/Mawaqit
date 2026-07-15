package com.hussein.mawaqit.presentation.qiblah

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.core.LocationRepository
import com.hussein.mawaqit.infrastructure.location.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class QiblahState(
    val compassHeading: Float = 0f,
    val qiblahAngle: Float = 0f,
    val accuracy: Int = 0 // 0: unreliable, 1: low, 2: medium, 3: high
)

class QiblahViewModel(
    private val locationRepository: LocationRepository,
    context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(QiblahState())
    val state = _state.asStateFlow()

    private val qiblahHelper = QiblahHelper(context) { heading, qiblahAngle, accuracy ->
        _state.value = QiblahState(heading, qiblahAngle, accuracy)
    }

    init {
        viewModelScope.launch {
            locationRepository.locationFlow.collect { savedLocation ->
                savedLocation?.let {
                    qiblahHelper.setUserLocation(
                        UserLocation(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            altitude = 0.0,
                            city = it.cityName
                        )
                    )
                }
            }
        }
    }

    fun start() = qiblahHelper.start()
    fun stop() = qiblahHelper.stop()

    override fun onCleared() {
        super.onCleared()
        qiblahHelper.stop()
    }
}

object QiblahCalculator {

    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    /**
     * Returns the bearing (0-360°, from true north) from the user's
     * location to the Kaaba using the great-circle formula.
     */
    fun getQiblahBearing(userLat: Double, userLng: Double): Double {
        val lat1 = toRadians(userLat)
        val lat2 = toRadians(KAABA_LAT)
        val deltaLng = toRadians(KAABA_LNG - userLng)

        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)

        val bearing = toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }
}

/**
 * Listens to device sensors and reports live compass heading + Qiblah bearing,
 * corrected for magnetic declination at the user's location.
 */
class QiblahHelper(
    context: Context,
    private val onUpdate: (compassHeading: Float, qiblahBearing: Float, accuracy: Int) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var qiblahBearing: Float = 0f
    private var magneticDeclination: Float = 0f

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var lastAzimuth = -1f // -1 means uninitialized
    private val smoothingFactor = 0.4f // Increased for faster responsiveness
    private var currentAccuracy = 0

    /** Call once you have the user's location (e.g. via FusedLocationProviderClient). */
    fun setUserLocation(location: UserLocation) {
        qiblahBearing = QiblahCalculator
            .getQiblahBearing(location.latitude, location.longitude)
            .toFloat()

        location.altitude?.let {
            magneticDeclination = GeomagneticField(
                location.latitude.toFloat(),
                location.longitude.toFloat(),
                location.altitude.toFloat(),
                System.currentTimeMillis()
            ).declination
        }
    }

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        var azimuth = toDegrees(orientationAngles[0].toDouble()).toFloat()
        azimuth += magneticDeclination // true north correction
        azimuth = (azimuth + 360) % 360

        // Initialize or filter
        if (lastAzimuth < 0) {
            lastAzimuth = azimuth
        } else {
            azimuth = lowPass(azimuth, lastAzimuth)
            lastAzimuth = azimuth
        }

        // Angle to rotate the Qiblah needle relative to current heading
        val relativeBearing = (qiblahBearing - azimuth + 360) % 360

        onUpdate(azimuth, relativeBearing, currentAccuracy)
    }

    private fun lowPass(newValue: Float, oldValue: Float): Float {
        var delta = newValue - oldValue
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return (oldValue + smoothingFactor * delta + 360) % 360
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        currentAccuracy = accuracy
    }
}
