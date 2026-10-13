/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.util

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.MediaRouter
import android.os.Build
import androidx.core.content.ContextCompat
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
    private fun resolveExternalRoute(): String? {
        _device.value?.let { return it.key }
        val outputs = if (Build.VERSION.SDK_INT >= 33 && !preferUsbOutput()) {
            audioManager.getAudioDevicesForAttributes(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
        } else audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        return outputs.firstOrNull { it.isSink && it.type in setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET
        ) }?.let { "wired:${it.type}:${it.id}" }
    }
    private val _externalRoute = MutableStateFlow(resolveExternalRoute())
    val externalRoute: StateFlow<String?> = _externalRoute.asStateFlow()
    private val settledRefresh = Runnable { refresh() }
    private fun routeEvent() {
        refresh()
        handler.removeCallbacks(settledRefresh)
        // The route broadcast can precede AudioPolicy's selected-output update.
        handler.postDelayed(settledRefresh, 750)
    }
    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { routeEvent() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) { routeEvent() }
    }
    private val playback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) { refresh() }
    }
    private val routeCallback = object : MediaRouter.SimpleCallback() {
        override fun onRouteSelected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) { routeEvent() }
        override fun onRouteUnselected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) { routeEvent() }
        override fun onRouteChanged(router: MediaRouter, info: MediaRouter.RouteInfo) { routeEvent() }
    }
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { routeEvent() }
    }
    init {
        audioManager.registerAudioDeviceCallback(devices, handler)
        audioManager.registerAudioPlaybackCallback(playback, handler)
        (context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter)?.addCallback(
            MediaRouter.ROUTE_TYPE_LIVE_AUDIO, routeCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS)
        ContextCompat.registerReceiver(context, bluetoothReceiver, IntentFilter().apply {
            addAction("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.action.LE_AUDIO_ACTIVE_DEVICE_CHANGED")
        }, ContextCompat.RECEIVER_EXPORTED)
    }
    fun refresh() {
        _usbConnected.value = usbAvailable()
        _device.value = resolveDevice()
        _externalRoute.value = resolveExternalRoute()
    }
}
