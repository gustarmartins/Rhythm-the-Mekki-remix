/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.util

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One application-owned observer for media-route changes, including speaker switches. */
class BluetoothDisplayDeviceMonitor(private val context: Context, private val preferUsbOutput: () -> Boolean = { false }) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private fun usbAvailable(): Boolean = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
        it.isSink && (it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET)
    }
    private val _usbConnected = MutableStateFlow(usbAvailable())
    val usbConnected: StateFlow<Boolean> = _usbConnected.asStateFlow()
    private fun resolveDevice(): BluetoothDisplayDevice? =
        if (preferUsbOutput() && usbAvailable()) null else AudioCapabilitiesMonitor.activeBluetoothOutputDevice(audioManager, context)
    private val _device = MutableStateFlow(resolveDevice())
    val device: StateFlow<BluetoothDisplayDevice?> = _device.asStateFlow()
    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { refresh() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) { refresh() }
    }
    private val playback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) { refresh() }
    }
    init {
        audioManager.registerAudioDeviceCallback(devices, handler)
        audioManager.registerAudioPlaybackCallback(playback, handler)
    }
    fun refresh() {
        _usbConnected.value = usbAvailable()
        _device.value = resolveDevice()
    }
}
