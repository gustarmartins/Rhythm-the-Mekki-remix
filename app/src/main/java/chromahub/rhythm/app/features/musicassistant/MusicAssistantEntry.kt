/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView

object MusicAssistantEntry {
    const val PROCESS_SUFFIX = ":music_assistant"
    const val WEBVIEW_DIRECTORY = "music_assistant"

    fun open(context: Context) {
        context.startActivity(Intent(context, MusicAssistantActivity::class.java))
    }

    /** Called before any main-process repositories or WebViews are initialized. */
    fun initializeProcess(context: Context): Boolean {
        val name = if (Build.VERSION.SDK_INT >= 28) Application.getProcessName() else {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            manager.runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
        }
        if (name != context.packageName + PROCESS_SUFFIX) return false
        if (Build.VERSION.SDK_INT >= 28) WebView.setDataDirectorySuffix(WEBVIEW_DIRECTORY)
        return true
    }
}
