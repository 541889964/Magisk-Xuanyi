package com.topjohnwu.magisk.ui

import android.Manifest
import android.Manifest.permission.REQUEST_INSTALL_PACKAGES
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.arch.VMFactory
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.base.ActivityExtension
import com.topjohnwu.magisk.core.base.SplashController
import com.topjohnwu.magisk.core.base.SplashScreenHost
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.wrap
import com.topjohnwu.magisk.ui.component.rememberConfirmDialog
import com.topjohnwu.magisk.ui.deny.DenyListScreen
import com.topjohnwu.magisk.ui.deny.DenyListViewModel
import com.topjohnwu.magisk.ui.flash.FlashScreen
import com.topjohnwu.magisk.ui.flash.FlashUtils
import com.topjohnwu.magisk.ui.flash.FlashViewModel
import com.topjohnwu.magisk.ui.module.ActionScreen
import com.topjohnwu.magisk.ui.module.ActionViewModel
import com.topjohnwu.magisk.ui.navigation.LocalNavigator
import com.topjohnwu.magisk.ui.navigation.Navigator
import com.topjohnwu.magisk.ui.navigation.Route
import com.topjohnwu.magisk.ui.navigation.rememberNavigator
import com.topjohnwu.magisk.ui.superuser.SuperuserDetailScreen
import com.topjohnwu.magisk.ui.superuser.SuperuserViewModel
import com.topjohnwu.magisk.view.Shortcuts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.topjohnwu.magisk.core.R as CoreR
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity(), SplashScreenHost {

    override val extension = ActivityExtension(this)
    override val splashController = SplashController(this)

    private val intentState = MutableStateFlow(0)
    internal val showInvalidState = MutableStateFlow(false)
    internal val showUnsupported = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    internal val showShortcutPrompt = MutableStateFlow(false)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.wrap())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        extension.onCreate(savedInstanceState)
        splashController.preOnCreate()
        theme.applyStyle(R.style.Main, true)
        super.onCreate(savedInstanceState)
        splashController.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        splashController.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        extension.onSaveInstanceState(outState)
    }

    @SuppressLint("InlinedApi")
    override fun onCreateUi(savedInstanceState: Bundle?) {
        showUnsupportedMessage()
        askForHomeShortcut()

        if (Config.checkUpdate) {
            extension.withPermission(Manifest.permission.POST_NOTIFICATIONS) {
                Config.checkUpdate = it
            }
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val initialTab = getInitialTab(intent)

        setContent { // [XuanYi-START]
        val xuanyiPrefs = getSharedPreferences("xuanyi", android.content.Context.MODE_PRIVATE)
        var bgAlpha by remember { mutableFloatStateOf(xuanyiPrefs.getFloat("xuanyi_bg_alpha", 1.0f)) }
        var bgBlur by remember { mutableFloatStateOf(xuanyiPrefs.getFloat("xuanyi_bg_blur", 0f)) }
        var showBgPanel by remember { mutableStateOf(false) }
        var showWelcome by remember { mutableStateOf(true) }
        var bgUri by remember { mutableStateOf<String?>(xuanyiPrefs.getString("xuanyi_bg_uri", null)) }
        var bgBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        val __ctx = LocalContext.current
        LaunchedEffect(bgUri) {
            bgBitmap = try {
                bgUri?.let { u ->
                    __ctx.contentResolver.openInputStream(android.net.Uri.parse(u))?.use { s ->
                        android.graphics.BitmapFactory.decodeStream(s)
                    }
                }
            } catch (e: Exception) { null }
        }
        val __imgLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                try { __ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
                bgUri = uri.toString()
                xuanyiPrefs.edit().putString("xuanyi_bg_uri", uri.toString()).apply()
            }
        }

            MagiskTheme {
                Box(modifier = Modifier.fillMaxSize()) {
            // [XuanYi] 背景图
            if (bgBitmap != null) {
                Image(
                    bitmap = asImageBitmap(bgBitmap!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().then(
                        if (android.os.Build.VERSION.SDK_INT >= 31 && bgBlur > 0.5f) Modifier.blur(bgBlur.dp) else Modifier
                    ),
                    contentScale = ContentScale.Crop,
                    alpha = bgAlpha
                )
            } else {
                Image(
                    painter = painterResource(id = com.topjohnwu.magisk.R.drawable.app_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().then(
                        if (android.os.Build.VERSION.SDK_INT >= 31 && bgBlur > 0.5f) Modifier.blur(bgBlur.dp) else Modifier
                    ),
                    contentScale = ContentScale.Crop,
                    alpha = bgAlpha
                )
            }

            // [XuanYi] 右上角 FAB（外层 Box 定位，不用 .align）
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 80.dp, end = 16.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .clickable { showBgPanel = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "◐", color = Color.White, fontSize = 22.sp)
                }
            }

                    val navigator = rememberNavigator(Route.Main)
                    CompositionLocalProvider(LocalNavigator provides navigator) {
                        HandleFlashIntent(navigator)

                        NavDisplay(
                            backStack = navigator.backStack,
                            onBack = { navigator.pop() },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator<Any>()
                            ),
                            entryProvider = entryProvider {
                                entry<Route.Main> {
                                    val superuserVm: SuperuserViewModel = viewModel(
                                        viewModelStoreOwner = this@MainActivity, factory = VMFactory
                                    )
                                    MainScreen(
                                        initialTab = initialTab,
                                        superuserViewModel = superuserVm,
                                        onAuthenticate = { action ->
                                            extension.withAuthentication { if (it) action() }
                                        }
                                    )
                                }
                                entry<Route.DenyList> { _ ->
                                    val vm: DenyListViewModel = viewModel(factory = VMFactory)
                                    LaunchedEffect(Unit) { vm.startLoading() }
                                    DenyListScreen(vm, onBack = { navigator.pop() })
                                }
                                entry<Route.Flash> { key ->
                                    val vm: FlashViewModel = viewModel(factory = VMFactory)
                                    LaunchedEffect(key) {
                                        if (vm.flashAction.isEmpty()) {
                                            vm.flashAction = key.action
                                            vm.flashUri = key.additionalData?.toUri()
                                            vm.startFlashing()
                                        }
                                    }
                                    FlashScreen(vm, onBack = { navigator.pop() })
                                }
                                entry<Route.SuperuserDetail> { key ->
                                    val vm: SuperuserViewModel = viewModel(
                                        viewModelStoreOwner = this@MainActivity, factory = VMFactory
                                    )
                                    LaunchedEffect(Unit) {
                                        vm.authenticate = { onSuccess ->
                                            extension.withAuthentication { if (it) onSuccess() }
                                        }
                                    }
                                    SuperuserDetailScreen(
                                        uid = key.uid,
                                        viewModel = vm,
                                        onBack = { navigator.pop() },
                                        onAuthenticate = { action ->
                                            extension.withAuthentication { if (it) action() }
                                        }
                                    )
                                }
                                entry<Route.Action> { key ->
                                    val vm: ActionViewModel = viewModel(factory = VMFactory)
                                    LaunchedEffect(key) {
                                        if (vm.actionId.isEmpty()) {
                                            vm.actionId = key.id
                                            vm.actionName = key.name
                                            vm.startRunAction()
                                        }
                                    }
                                    ActionScreen(vm, actionName = key.name, onBack = { navigator.pop() })
                                }
                            }
                        )
                    }
                    MainActivityDialogs(
                        showInvalid = showInvalidState.collectAsStateWithLifecycle().value,
                        unsupportedMessages = showUnsupported.collectAsStateWithLifecycle().value,
                        showShortcut = showShortcutPrompt.collectAsStateWithLifecycle().value,
                        onInvalidConfirmed = {
                            showInvalidState.value = false
                            handleInvalidStateInstall()
                        },
                        onShortcutConfirmed = {
                            showShortcutPrompt.value = false
                            Shortcuts.addHomeIcon(this@MainActivity)
                        },
                        onShortcutDismissed = {
                            showShortcutPrompt.value = false
                        }
                    )
                
            // [XuanYi] 面板
            if (showBgPanel) {
                AlertDialog(
                    onDismissRequest = { showBgPanel = false },
                    title = { Text("🎨 背景外观", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("透明度", fontWeight = FontWeight.Medium)
                                Text("${(bgAlpha * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(value = bgAlpha, onValueChange = { bgAlpha = it; xuanyiPrefs.edit().putFloat("xuanyi_bg_alpha", it).apply() }, valueRange = 0f..1f, steps = 19)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("模糊度", fontWeight = FontWeight.Medium)
                                Text(if (android.os.Build.VERSION.SDK_INT >= 31) "${bgBlur.toInt()} dp" else "需 12+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(value = bgBlur, onValueChange = { bgBlur = it; xuanyiPrefs.edit().putFloat("xuanyi_bg_blur", it).apply() }, valueRange = 0f..30f, steps = 29, enabled = android.os.Build.VERSION.SDK_INT >= 31)
                        }
                    },
                    confirmButton = { TextButton(onClick = { __imgLauncher.launch("image/*") }) { Text("从本地选图") } },
                    dismissButton = { TextButton(onClick = { showBgPanel = false }) { Text("完成") } }
                )
            }
}
            }
        
        // [XuanYi-END]
        if (showWelcome) {
            XuanYiWelcomeDialog(onAccept = { showWelcome = false })
        }
}
    }

    @Composable
    private fun HandleFlashIntent(navigator: Navigator) {
        val intentVersion by intentState.collectAsStateWithLifecycle()
        LaunchedEffect(intentVersion) {
            val currentIntent = intent ?: return@LaunchedEffect
            if (currentIntent.action == FlashUtils.INTENT_FLASH) {
                val action = currentIntent.getStringExtra(FlashUtils.EXTRA_FLASH_ACTION)
                    ?: return@LaunchedEffect
                val uri = currentIntent.getStringExtra(FlashUtils.EXTRA_FLASH_URI)
                navigator.push(Route.Flash(action, uri))
                currentIntent.action = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value += 1
    }

    private fun getInitialTab(intent: Intent?): Int {
        val section = when {
            intent?.action == Intent.ACTION_APPLICATION_PREFERENCES -> Const.Nav.SETTINGS
            intent?.action == FlashUtils.INTENT_FLASH &&
                intent.getStringExtra(FlashUtils.EXTRA_FLASH_ACTION) == Const.Value.FLASH_ZIP -> Const.Nav.MODULES
            else -> intent?.getStringExtra(Const.Key.OPEN_SECTION)
        }
        return when (section) {
            Const.Nav.SUPERUSER -> Tab.SUPERUSER.ordinal
            Const.Nav.MODULES -> Tab.MODULES.ordinal
            Const.Nav.SETTINGS -> Tab.SETTINGS.ordinal
            else -> Tab.HOME.ordinal
        }
    }

    @SuppressLint("InlinedApi")
    override fun showInvalidStateMessage() {
        showInvalidState.value = true
    }

    internal fun handleInvalidStateInstall() {
        extension.withPermission(REQUEST_INSTALL_PACKAGES) {
            if (!it) {
                toast(CoreR.string.install_unknown_denied, Toast.LENGTH_SHORT)
                showInvalidState.value = true
            } else {
                lifecycleScope.launch {
                    if (!AppMigration.restoreApp(this@MainActivity)) {
                        toast(CoreR.string.failure, Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    private fun showUnsupportedMessage() {
        val messages = mutableListOf<Pair<Int, Int>>()

        if (Info.env.isUnsupported) {
            messages.add(CoreR.string.unsupport_magisk_title to CoreR.string.unsupport_magisk_msg)
        }
        if (!Info.isEmulator && Info.env.isActive && System.getenv("PATH")
                ?.split(':')
                ?.filterNot { File("$it/magisk").exists() }
                ?.any { File("$it/su").exists() } == true) {
            messages.add(CoreR.string.unsupport_general_title to CoreR.string.unsupport_other_su_msg)
        }
        if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            messages.add(CoreR.string.unsupport_general_title to CoreR.string.unsupport_system_app_msg)
        }
        if (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) {
            messages.add(CoreR.string.unsupport_general_title to CoreR.string.unsupport_external_storage_msg)
        }

        if (messages.isNotEmpty()) {
            showUnsupported.value = messages
        }
    }

    private fun askForHomeShortcut() {
        if (isRunningAsStub && !Config.askedHome &&
            ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Config.askedHome = true
            showShortcutPrompt.value = true
        }
    }
}

@Composable
private fun MainActivityDialogs(
    showInvalid: Boolean,
    unsupportedMessages: List<Pair<Int, Int>>,
    showShortcut: Boolean,
    onInvalidConfirmed: () -> Unit,
    onShortcutConfirmed: () -> Unit,
    onShortcutDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val invalidDialog = rememberConfirmDialog(
        onConfirm = onInvalidConfirmed,
        onDismiss = {}
    )

    LaunchedEffect(showInvalid) {
        if (showInvalid) {
            invalidDialog.showConfirm(
                title = resources.getString(CoreR.string.unsupport_nonroot_stub_title),
                content = resources.getString(CoreR.string.unsupport_nonroot_stub_msg),
                confirm = resources.getString(CoreR.string.install),
            )
        }
    }

    var currentUnsupportedIndex by rememberSaveable { mutableIntStateOf(0) }
    val unsupportedDialog = rememberConfirmDialog(
        onConfirm = { currentUnsupportedIndex++ },
        onDismiss = { currentUnsupportedIndex++ }
    )

    val currentUnsupported = unsupportedMessages.getOrNull(currentUnsupportedIndex)
    LaunchedEffect(currentUnsupported) {
        if (currentUnsupported != null) {
            val (titleRes, msgRes) = currentUnsupported
            unsupportedDialog.showConfirm(
                title = resources.getString(titleRes),
                content = resources.getString(msgRes),
            )
        }
    }

    val shortcutDialog = rememberConfirmDialog(
        onConfirm = onShortcutConfirmed,
        onDismiss = onShortcutDismissed
    )

    LaunchedEffect(showShortcut) {
        if (showShortcut) {
            shortcutDialog.showConfirm(
                title = resources.getString(CoreR.string.add_shortcut_title),
                content = resources.getString(CoreR.string.add_shortcut_msg),
            )
        }
    }
}
