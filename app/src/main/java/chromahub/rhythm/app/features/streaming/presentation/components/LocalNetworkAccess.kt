/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import chromahub.rhythm.app.R

/** Request the Android 17 LAN permission in response to Connect/Discover, then retry that action. */
@Composable
fun rememberLocalNetworkAccess(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending
        pending = null
        if (granted) action?.invoke()
        else Toast.makeText(context, R.string.local_network_access_required, Toast.LENGTH_LONG).show()
    }
    return { action ->
        if (Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pending = action
            launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
