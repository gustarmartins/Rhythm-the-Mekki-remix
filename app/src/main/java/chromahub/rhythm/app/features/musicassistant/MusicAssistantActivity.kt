/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.ui.theme.RhythmTheme

/** Application-context binding survives rotation; no Activity is retained by the ViewModel. */
class MusicAssistantConnection(application: Application) : AndroidViewModel(application), ServiceConnection {
    private val prefs = application.getSharedPreferences("music_assistant", Context.MODE_PRIVATE)
    var address by mutableStateOf(prefs.getString("server", "").orEmpty())
    var savedAddress = address
        private set
    var inSession by mutableStateOf(false)
    var service by mutableStateOf<MusicAssistantService?>(null)
    var visible = false
    var bindFailed by mutableStateOf(false)
    private val bound = Build.VERSION.SDK_INT >= 28 && application.bindService(
        Intent(application, MusicAssistantService::class.java), this, Context.BIND_AUTO_CREATE
    )

    init { bindFailed = Build.VERSION.SDK_INT >= 28 && !bound }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service = (binder as MusicAssistantService.LocalBinder).service
        if (service?.webView != null) {
            address = service!!.server
            savedAddress = address
            inSession = true
        } else if (inSession && visible) service?.resumeVisibleSession(savedAddress)
    }
    override fun onServiceDisconnected(name: ComponentName?) { service = null }
    override fun onBindingDied(name: ComponentName?) { service = null; bindFailed = true }
    fun save(normalized: String) {
        address = normalized
        savedAddress = normalized
        prefs.edit { putString("server", normalized) }
    }
    fun forget() {
        service?.forget {
            prefs.edit { clear() }
            address = ""
            savedAddress = ""
            inSession = false
        }
    }
    override fun onCleared() {
        if (bound) getApplication<Application>().unbindService(this)
    }
}

class MusicAssistantActivity : ComponentActivity() {
    private lateinit var connection: MusicAssistantConnection
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Foreground playback is allowed with permission denied, with Android's active-app indicator.
        enableBackground()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Old shortcuts opened this activity directly. Their default is now native Rhythm.
        if (!intent.getBooleanExtra(MusicAssistantEntry.SERVER_ADMINISTRATION, false)) {
            MusicAssistantEntry.open(this)
            finish()
            return
        }
        enableEdgeToEdge()
        connection = ViewModelProvider(this)[MusicAssistantConnection::class.java]
        setContent { RhythmTheme(dynamicColor = true) { Workspace() } }
    }

    override fun onStart() {
        super.onStart()
        if (!::connection.isInitialized) return
        connection.visible = true
        if (connection.inSession) connection.service?.resumeVisibleSession(connection.savedAddress)
    }

    override fun onStop() {
        if (::connection.isInitialized) {
            connection.visible = false
            connection.service?.uiHidden(isChangingConfigurations)
        }
        super.onStop()
    }

    private fun enableBackground() {
        if (!connection.visible || connection.service?.webView == null) return
        try {
            ContextCompat.startForegroundService(this, Intent(this, MusicAssistantService::class.java)
                .setAction(MusicAssistantService.ACTION_BACKGROUND))
        } catch (_: RuntimeException) {
            android.widget.Toast.makeText(this, R.string.music_assistant_background_error, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun openBrowser(url: String) {
        if (!MusicAssistantServer.externalLink(url)) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(this, R.string.music_assistant_no_browser, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Workspace() {
        val service = connection.service
        var editing by remember { mutableStateOf(false) }
        var help by remember { mutableStateOf(false) }
        var confirmForget by remember { mutableStateOf(false) }
        var invalidAddress by remember { mutableStateOf(false) }
        val showingPlayer = connection.inSession && !editing

        fun goBack() {
            when {
                editing -> { connection.address = connection.savedAddress; editing = false }
                service?.webView?.canGoBack() == true -> service.webView?.goBack()
                else -> finish()
            }
        }
        BackHandler { goBack() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.ma_admin)) },
                    navigationIcon = { IconButton(onClick = { goBack() }) {
                        Icon(MaterialSymbolIcon("arrow_back"), stringResource(R.string.music_assistant_back))
                    } },
                    actions = {
                        IconButton(onClick = { help = true }) {
                            Icon(MaterialSymbolIcon("help"), stringResource(R.string.music_assistant_help))
                        }
                        if (showingPlayer) {
                            IconButton(onClick = { service?.retry() }) {
                                Icon(MaterialSymbolIcon("refresh"), stringResource(R.string.music_assistant_reload))
                            }
                            IconButton(onClick = { editing = true }) {
                                Icon(MaterialSymbolIcon("settings"), stringResource(R.string.music_assistant_server_settings))
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                if (!showingPlayer) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.music_assistant_description), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.music_assistant_setup_help))
                        OutlinedTextField(
                            value = connection.address,
                            onValueChange = { connection.address = it; invalidAddress = false },
                            label = { Text(stringResource(R.string.music_assistant_server_url)) },
                            placeholder = { Text("http://raspberrypi:8095") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            isError = invalidAddress,
                            supportingText = { if (invalidAddress) Text(stringResource(R.string.music_assistant_invalid_url)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (Build.VERSION.SDK_INT < 28) Text(stringResource(R.string.music_assistant_android8))
                        if (connection.bindFailed) Text(stringResource(R.string.music_assistant_background_error))
                        Button(
                            enabled = Build.VERSION.SDK_INT < 28 || (service != null && !service.clearing),
                            onClick = {
                                val normalized = MusicAssistantServer.normalize(connection.address)
                                if (normalized == null) invalidAddress = true
                                else {
                                    connection.save(normalized)
                                    if (Build.VERSION.SDK_INT < 28) openBrowser(normalized)
                                    else {
                                        service?.open(normalized)
                                        connection.inSession = true
                                        editing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.music_assistant_open)) }
                        if (service?.webView != null) {
                            BackgroundSetting(service)
                            OutlinedButton(onClick = {
                                service.releasePlayer()
                                connection.inSession = false
                                editing = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.music_assistant_stop_player))
                            }
                        }
                        if (connection.address.isNotEmpty()) TextButton(
                            onClick = { confirmForget = true }, enabled = service != null && !service.clearing
                        ) { Text(stringResource(R.string.music_assistant_forget)) }
                    }
                } else {
                    if (service?.loading == true) LinearProgressIndicator(Modifier.fillMaxWidth())
                    service?.error?.let { message ->
                        Surface(color = MaterialTheme.colorScheme.errorContainer) {
                            Text(stringResource(message), Modifier.fillMaxWidth().padding(16.dp))
                        }
                    }
                    val webView = service?.webView
                    if (webView == null) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.music_assistant_player_stopped))
                            Button(onClick = { service?.open(connection.savedAddress) }, enabled = service != null) {
                                Text(stringResource(R.string.music_assistant_open))
                            }
                        }
                    } else {
                        AndroidView(
                            factory = { context -> FrameLayout(context) },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onRelease = { container -> container.removeAllViews() },
                            update = { container ->
                                if (webView.parent !== container) {
                                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                                    service.attach(this@MusicAssistantActivity)
                                    container.removeAllViews()
                                    container.addView(webView, FrameLayout.LayoutParams(-1, -1))
                                }
                            }
                        )
                        BackgroundSetting(service)
                    }
                }
            }
        }

        if (help) AlertDialog(
            onDismissRequest = { help = false },
            title = { Text(stringResource(R.string.music_assistant_help)) },
            text = { Text(stringResource(R.string.music_assistant_usage_help), Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { help = false }) { Text(stringResource(R.string.music_assistant_done)) } }
        )
        if (confirmForget) AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text(stringResource(R.string.music_assistant_forget)) },
            text = { Text(stringResource(R.string.music_assistant_forget_description)) },
            confirmButton = { TextButton(onClick = {
                connection.forget()
                confirmForget = false
                editing = false
            }) { Text(stringResource(R.string.music_assistant_forget)) } },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text(stringResource(R.string.music_assistant_cancel)) } }
        )
        service?.externalUrl?.let { url ->
            AlertDialog(
                onDismissRequest = { service.externalUrl = null },
                title = { Text(stringResource(R.string.music_assistant_external_title)) },
                text = { Text(stringResource(R.string.music_assistant_external_description)) },
                confirmButton = { TextButton(onClick = { service.externalUrl = null; openBrowser(url) }) {
                    Text(stringResource(R.string.music_assistant_browser))
                } },
                dismissButton = { TextButton(onClick = { service.externalUrl = null }) { Text(stringResource(R.string.music_assistant_cancel)) } }
            )
        }
    }

    @Composable
    private fun BackgroundSetting(service: MusicAssistantService) {
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.music_assistant_background), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.music_assistant_background_description), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = service.backgroundEnabled, onCheckedChange = { enabled ->
                    if (!enabled) service.disableBackground()
                    else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MusicAssistantActivity, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else enableBackground()
                })
            }
        }
    }
}
