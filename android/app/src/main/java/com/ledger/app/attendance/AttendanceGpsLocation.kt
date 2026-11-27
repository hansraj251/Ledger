package com.ledger.app.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AttendanceLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val isMock: Boolean
)

/**
 * Requests a fresh one-shot location. A recent cached fix is used only as a
 * fallback; the server still decides whether the fix is accurate enough.
 */
suspend fun readAttendanceLocation(context: Context): AttendanceLocationFix? {
    val hasFine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return null

    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val candidateProviders = if (hasFine) {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    } else {
        listOf(LocationManager.NETWORK_PROVIDER)
    }
    val providers = candidateProviders.filter { provider ->
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
    }

    for (provider in providers) {
        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            withTimeoutOrNull(10_000L) {
                suspendCancellableCoroutine { continuation ->
                    val cancellation = CancellationSignal()
                    continuation.invokeOnCancellation { cancellation.cancel() }
                    try {
                        manager.getCurrentLocation(
                            provider,
                            cancellation,
                            context.mainExecutor
                        ) { result ->
                            if (continuation.isActive) continuation.resume(result)
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
        } else {
            withTimeoutOrNull(10_000L) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) continuation.resume(location)
                        }

                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                        @Suppress("DEPRECATION")
                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: android.os.Bundle?
                        ) = Unit
                    }
                    try {
                        @Suppress("DEPRECATION")
                        manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                        continuation.invokeOnCancellation {
                            runCatching { manager.removeUpdates(listener) }
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
        }
        if (location != null) return location.toAttendanceFix()
    }

    // Cached fallback must be recent. It is still subject to server accuracy checks.
    val nowNanos = SystemClock.elapsedRealtimeNanos()
    return providers.mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.filter { location ->
        location.hasAccuracy() &&
            nowNanos >= location.elapsedRealtimeNanos &&
            nowNanos - location.elapsedRealtimeNanos <= 30_000_000_000L
    }.minByOrNull { it.accuracy }?.toAttendanceFix()
}

private fun Location.toAttendanceFix(): AttendanceLocationFix {
    val mock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        @Suppress("DEPRECATION")
        isFromMockProvider
    }
    return AttendanceLocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else 10_000.0,
        isMock = mock
    )
}
