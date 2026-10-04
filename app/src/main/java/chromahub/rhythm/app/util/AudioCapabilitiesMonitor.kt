/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Monitor audio device changes
 * Detects when audio output devices connect/disconnect
 */
class AudioCapabilitiesMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioCapabilitiesMonitor"

        fun activeBluetoothOutputName(audioManager: AudioManager): String? =
            activeBluetoothOutputDevice(audioManager)?.name

        /** Query the media route rather than treating every connected device as active. */
        fun activeBluetoothOutputDevice(audioManager: AudioManager, context: Context? = null): BluetoothDisplayDevice? {
            return try {
                val outputs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    audioManager.getAudioDevicesForAttributes(attributes)
                } else {
                    // Older Android exposes its selected media route through MediaRouter.
                    val router = context?.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? android.media.MediaRouter
                    val route = router?.getSelectedRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                    if (route?.deviceType != android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH) return null
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                }
                val device = outputs.firstOrNull { it.isSink && isBluetoothOutputType(it.type) }
                    ?: return null
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address else null
                val remoteName = try {
                    address?.takeIf { it.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")) }?.let {
                        @Suppress("DEPRECATION")
                        BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(it)?.name
                    }
                } catch (_: SecurityException) { null } catch (_: IllegalArgumentException) { null }
                val routeName = (context?.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? android.media.MediaRouter)
                    ?.getSelectedRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                    ?.takeIf { it.deviceType == android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH }
                    ?.name?.toString()?.trim()?.takeIf { it.isNotBlank() }
                val productName = device.productName?.toString()?.trim()
                    ?.takeIf { it.isNotBlank() && !it.equals(Build.MODEL, ignoreCase = true) }
                BluetoothDisplayDevice.from(address, remoteName?.trim()?.takeIf { it.isNotBlank() } ?: routeName ?: productName ?: "Bluetooth")
            } catch (e: Exception) {
                Log.w(TAG, "Unable to resolve Bluetooth media route", e)
                null
            }
        }

        fun isBluetoothOutputType(type: Int): Boolean =
            type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && type == AudioDeviceInfo.TYPE_BLE_BROADCAST)

    }
    
    interface Listener {
        fun onAudioDeviceChanged(deviceType: String)
    }
    
    private var listener: Listener? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    when (state) {
                        0 -> {
                            Log.d(TAG, "Headset unplugged")
                            listener?.onAudioDeviceChanged("Headset disconnected")
                        }
                        1 -> {
                            Log.d(TAG, "Headset plugged in")
                            listener?.onAudioDeviceChanged("Headset connected")
                        }
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d(TAG, "Audio becoming noisy (headphones disconnected)")
                    listener?.onAudioDeviceChanged("Audio device disconnected")
                }
            }
        }
    }

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    init {
        // AudioDeviceCallback is a public API since API 23
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(devices: Array<out AudioDeviceInfo>) {
                        devices.forEach { device ->
                    val deviceName = getDeviceName(device)
                                Log.d(TAG, "Audio device added: $deviceName")
                                listener?.onAudioDeviceChanged("$deviceName connected")
                            }
                        }

            override fun onAudioDevicesRemoved(devices: Array<out AudioDeviceInfo>) {
                        devices.forEach { device ->
                    val deviceName = getDeviceName(device)
                                Log.d(TAG, "Audio device removed: $deviceName")
                                listener?.onAudioDeviceChanged("$deviceName disconnected")
                            }
                        }
                    }
                }
    
    private fun getDeviceName(device: AudioDeviceInfo): String {
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            else -> "Audio Device"
        }
    }
    
    /**
     * Start monitoring audio device changes
     */
    fun startMonitoring(listener: Listener) {
        this.listener = listener
        Log.d(TAG, "Starting audio device monitoring")
        
        // Register broadcast receiver for headset plug events
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        context.registerReceiver(headsetReceiver, filter)
        
        // Register audio device callback (public API since API 23)
            registerAudioDeviceCallbackCompat()
        }
    
    private fun registerAudioDeviceCallbackCompat() {
        try {
            audioDeviceCallback?.let { audioManager.registerAudioDeviceCallback(it, mainHandler) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register audio device callback", e)
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping audio device monitoring")
        
        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering headset receiver", e)
        }
        
            unregisterAudioDeviceCallbackCompat()
        
        listener = null
    }
    
    private fun unregisterAudioDeviceCallbackCompat() {
        try {
            audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister audio device callback", e)
        }
    }
}
