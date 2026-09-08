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

        /** Returns the active A2DP/SCO device name, or null for non-Bluetooth output. */
        fun activeBluetoothOutputName(audioManager: AudioManager): String? {
            return try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    @Suppress("DEPRECATION")
                    return if (audioManager.isBluetoothA2dpOn) "Bluetooth" else null
                }

                val device = audioManager
                    .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .firstOrNull {
                        it.isSink && (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
                    } ?: return null

                // Some devices report the handset model as the sink product name.
                val remoteName = try {
                    // AudioDeviceInfo.address was added in API 28. Keep API 26/27
                    // Bluetooth output detection functional by falling back to productName.
                    val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        device.address
                    } else {
                        null
                    }
                    address
                        ?.takeIf { it.contains(':') }
                        ?.let {
                            @Suppress("DEPRECATION")
                            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(it)?.name
                        }
                } catch (e: SecurityException) {
                    null // BLUETOOTH_CONNECT not granted
                } catch (e: IllegalArgumentException) {
                    null // not a valid MAC
                }

                val productName = device.productName?.toString()?.trim()
                    ?.takeIf { it.isNotBlank() && !it.equals(Build.MODEL, ignoreCase = true) }

                remoteName?.trim()?.takeIf { it.isNotBlank() }
                    ?: productName
                    ?: "Bluetooth"
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve active Bluetooth output name", e)
                null
            }
        }
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
