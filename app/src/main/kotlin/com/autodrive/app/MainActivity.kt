package com.autodrive.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autodrive.app.core.sync.data.SyncManager
import com.autodrive.app.core.platform.notifications.AutoDriveNotificationConstants
import com.autodrive.app.PermissionsDeniedDialog
import com.autodrive.app.navigation.AppNavigation
import com.autodrive.app.navigation.AppNavigationViewModel
import com.autodrive.app.navigation.Screen
import com.autodrive.app.feature.auth.presentation.splash.SplashViewModel
import com.autodrive.app.feature.auth.presentation.splash.SplashDestination
import com.autodrive.app.core.designsystem.theme.AutoDriveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncManager: SyncManager

    private val appNavVm: AppNavigationViewModel by viewModels()

    private val _pendingNavRoute = Channel<String>(Channel.BUFFERED)
    private val pendingNavRoute  = _pendingNavRoute.receiveAsFlow()

    private var onPermissionsResult: ((Boolean) -> Unit)? = null

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val anyDenied = results.values.any { !it }
            onPermissionsResult?.invoke(anyDenied)
        }

    private fun buildPermissionsList(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        handleIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                runCatching { syncManager.touchPresence(force = true) }
                while (isActive) {
                    delay(PRESENCE_HEARTBEAT_MS)
                    runCatching { syncManager.touchPresence() }
                }
            }
        }

        setContent {
            AutoDriveTheme {
                var showDeniedDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    onPermissionsResult = { anyDenied ->
                        if (anyDenied) showDeniedDialog = true
                    }
                    val permissions = buildPermissionsList()
                    if (permissions.isNotEmpty()) {
                        requestPermissions.launch(permissions)
                    }
                }

                if (showDeniedDialog) {
                    PermissionsDeniedDialog(
                        onContinue = { showDeniedDialog = false },
                        onGrant = {
                            showDeniedDialog = false
                            openAppSettings()
                        }
                    )
                }

                val splashVm: SplashViewModel = hiltViewModel()
                val startDest by splashVm.startDest.collectAsState()

                startDest?.let { dest ->
                    AppNavigation(
                        startDestination = when (dest) {
                            SplashDestination.PHONE_INPUT -> Screen.PhoneInput.route
                            SplashDestination.WAITING -> Screen.Waiting.route
                            SplashDestination.HOME -> Screen.Home.route
                            SplashDestination.REGISTRATION -> Screen.BasicInfo.createRoute(
                                appNavVm.accountType.ifBlank { "MARKETER" }
                            )
                        },
                        navVm            = appNavVm,
                        pendingNavRoute  = pendingNavRoute
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val route = intent.getStringExtra(AutoDriveNotificationConstants.EXTRA_NAV_ROUTE)
            ?: intent.getStringExtra(AutoDriveNotificationConstants.DATA_NAV_ROUTE)
            ?: AutoDriveNotificationConstants.routeForType(
                intent.getStringExtra(AutoDriveNotificationConstants.DATA_TYPE)
            )
        route?.let { _pendingNavRoute.trySend(it) }
    }

    private companion object {
        const val PRESENCE_HEARTBEAT_MS = 2 * 60 * 1000L
    }
}
