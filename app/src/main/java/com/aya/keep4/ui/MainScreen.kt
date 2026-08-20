package com.aya.keep4.ui

import android.Manifest
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aya.keep4.MainActivity
import com.aya.keep4.data.AppState
import com.aya.keep4.data.SettingsStore
import kotlinx.coroutines.delay
import com.aya.keep4.keepalive.KeepAlive
import com.aya.keep4.keepalive.KeepAliveMode
import com.aya.keep4.keepalive.RootHelper
import com.aya.keep4.keepalive.OemUtils
import com.aya.keep4.keepalive.ShizukuHelper
import rikka.shizuku.Shizuku
import com.aya.keep4.service.SpeakerService
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Back


import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class Screen { HOME, SETTINGS }

/** ????????????????????????????????? */
private val ContentMaxWidth = 640.dp

private const val REQ_SHIZUKU_PERMISSION = 1001


@Composable
fun MainScreen() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen == Screen.SETTINGS) {
        screen = Screen.HOME
    }
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState == Screen.SETTINGS) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 4 } + fadeOut())
            }
        },
        label = "screen"
    ) { s ->
        when (s) {
            Screen.HOME -> HomeScreen(onOpenSettings = { screen = Screen.SETTINGS })
            Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
        }
    }
}

@Composable
private fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context.applicationContext) }

    var enabled by remember { mutableStateOf(settings.enabled) }
    var pendingEnable by remember { mutableStateOf(false) }

    val serviceRunning by AppState.serviceRunning.collectAsState()
    val triggerActive by AppState.triggerActive.collectAsState()
    val playbackActive by AppState.playbackActive.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val recordGranted = grants[Manifest.permission.RECORD_AUDIO] == true
        if (pendingEnable) {
            pendingEnable = false
            if (recordGranted) {
                enabled = true
                settings.enabled = true
                if (!context.startKeep4Service()) {
                    enabled = false
                    settings.enabled = false
                }
            } else {
                Toast.makeText(context, "需要麦克风权限才能开启四扬", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 首次运行：直接请求麦克风 + 通知两个必要权限
    LaunchedEffect(Unit) {
        if (!settings.permissionsPrompted) {
            settings.permissionsPrompted = true
            val missing = requiredPermissions(context).filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    fun toggle(newValue: Boolean) {
        if (newValue) {
            val missing = requiredPermissions(context).filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                enabled = true
                settings.enabled = true
                if (!context.startKeep4Service()) {
                    enabled = false
                    settings.enabled = false
                }
            } else {
                pendingEnable = true
                permissionLauncher.launch(missing.toTypedArray())
            }
        } else {
            pendingEnable = false
            enabled = false
            settings.enabled = false
            context.stopService(Intent(context, SpeakerService::class.java))
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Keep4",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = MiuixIcons.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .widthIn(max = ContentMaxWidth)
                    .padding(top = paddingValues.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                SmallTitle(text = "状态")
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (serviceRunning) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (serviceRunning) MiuixIcons.Basic.Check else MiuixIcons.Basic.Close,
                                contentDescription = null,
                                tint = if (serviceRunning) MiuixTheme.colorScheme.onPrimary
                                else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "保活服务",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            Text(
                                text = if (serviceRunning) "运行中" else "已停止",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (serviceRunning) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                    HorizontalDivider()
                    MiniStatusRow("系统播放", if (playbackActive) "有音频播放" else "无播放", playbackActive)
                    MiniStatusRow("四扬触发", if (triggerActive) "已生效" else "未触发", triggerActive)
                }

                SmallTitle(text = "四扬声器")
                Card {
                    SwitchPreference(
                        title = "开启四扬声器",
                        summary = when {
                            enabled && serviceRunning && triggerActive -> "运行中：检测到播放，保持四扬"
                            enabled && serviceRunning -> "运行中：等待播放"
                            enabled -> "已开启（服务启动中…）"
                            else -> "关闭"
                        },
                        checked = enabled,
                        onCheckedChange = ::toggle
                    )
                }

                SmallTitle(text = "说明")
                Card {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "让联想小新 Pad Pro 12.7 在刷入第三方 ROM 后强制开启四扬声器。\n" +
                                "原理：播放音乐时占用麦克风（或切入通信路由），把系统顶进四个扬声器全部驱动的音频模式。\n" +
                                "请开启所有权限，并在任务管理中将本应用加入后台锁定，避免被杀。\n" +
                                "更多设置（触发方式、权限、反馈）请点击右上角设置图标。",
                            style = MiuixTheme.textStyles.body2
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context.applicationContext) }

    var commMode by remember { mutableStateOf(settings.communicationMode) }
    var hideRecents by remember { mutableStateOf(settings.hideFromRecents) }
    var showDisclaimer by remember { mutableStateOf(false) }
    var keepAliveMode by remember { mutableStateOf(KeepAliveMode.fromId(settings.keepAliveMode)) }
    var accEnabled by remember { mutableStateOf(KeepAlive.isAccessibilityServiceEnabled(context)) }
    var shizukuReady by remember { mutableStateOf(ShizukuHelper.isReady()) }
    var rootAvailable by remember { mutableStateOf(RootHelper.isAvailable()) }
    var ignoringBattery by remember { mutableStateOf(OemUtils.isIgnoringBatteryOptimizations(context)) }
    var showRootWarning by remember { mutableStateOf(false) }
    var rootCountdown by remember { mutableIntStateOf(5) }

    var micGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO)) }
    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    // 从系统设置返回时刷新权限状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                micGranted = context.hasPermission(Manifest.permission.RECORD_AUDIO)
                notifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                accEnabled = KeepAlive.isAccessibilityServiceEnabled(context)
                shizukuReady = ShizukuHelper.isReady()
                rootAvailable = RootHelper.isAvailable()
                ignoringBattery = OemUtils.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Shizuku 授权结果监听
    val shizukuListener = remember {
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQ_SHIZUKU_PERMISSION) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    shizukuReady = true
                    applyPrivilegedWhitelist(context, "Shizuku") { ShizukuHelper.run(it) }
                } else {
                    Toast.makeText(context, "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    DisposableEffect(Unit) {
        ShizukuHelper.addPermissionListener(shizukuListener)
        onDispose { ShizukuHelper.removePermissionListener(shizukuListener) }
    }

    fun selectKeepAliveMode(mode: KeepAliveMode) {
        // Root 需先阅读警告并倒计时，同意后才真正开启
        if (mode == KeepAliveMode.ROOT) {
            if (RootHelper.isAvailable()) {
                showRootWarning = true
                rootCountdown = 5
            } else {
                Toast.makeText(context, "未检测到 Root（Magisk）", Toast.LENGTH_LONG).show()
            }
            return
        }
        keepAliveMode = mode
        settings.keepAliveMode = mode.id
        when (mode) {
            KeepAliveMode.FOREGROUND -> { /* 默认模式，无需额外配置 */ }
            KeepAliveMode.ACCESSIBILITY -> {
                if (!KeepAlive.isAccessibilityServiceEnabled(context)) {
                    Toast.makeText(
                        context,
                        "请在系统无障碍设置中开启 Keep4 保活服务",
                        Toast.LENGTH_LONG
                    ).show()
                    context.openAccessibilitySettings()
                }
            }
            KeepAliveMode.SHIZUKU -> {
                when {
                    !ShizukuHelper.isBinderReady() ->
                        Toast.makeText(context, "Shizuku 未运行，请先启动 Shizuku", Toast.LENGTH_LONG).show()
                    !ShizukuHelper.isPermissionGranted() -> {
                        Toast.makeText(context, "请在 Shizuku 弹窗中允许授权", Toast.LENGTH_LONG).show()
                        ShizukuHelper.requestPermission(REQ_SHIZUKU_PERMISSION)
                    }
                    else -> applyPrivilegedWhitelist(context, "Shizuku") { ShizukuHelper.run(it) }
                }
            }
            KeepAliveMode.ROOT -> { /* 走上方警告流程 */ }
        }
    }



    // Root warning countdown: force 5s reading before enabling
    LaunchedEffect(showRootWarning) {
        if (showRootWarning) {
            rootCountdown = 5
            while (rootCountdown > 0) {
                delay(1000)
                rootCountdown--
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .widthIn(max = ContentMaxWidth)
                    .padding(top = paddingValues.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                SmallTitle(text = "触发方式")
                Card {
                    SwitchPreference(
                        title = "免录音模式（通信模式）",
                        summary = "不占用麦克风，用通话路由触发；MIUI/HyperOS 可能无效，无效请切回录音模式",
                        checked = commMode,
                        onCheckedChange = { newValue ->
                            commMode = newValue
                            settings.communicationMode = newValue
                            SpeakerService.restartTriggerIfNeeded()
                            Toast.makeText(context, "已切换，立即生效", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SwitchPreference(
                        title = "隐藏后台",
                        summary = "从最近任务中隐藏本应用",
                        checked = hideRecents,
                        onCheckedChange = { newValue ->
                            hideRecents = newValue
                            settings.hideFromRecents = newValue
                            val ok = context.setExcludeFromRecents(newValue)
                            Toast.makeText(
                                context,
                                if (ok) (if (newValue) "已从最近任务隐藏" else "已恢复显示")
                                else "当前系统不支持运行时切换",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                SmallTitle(text = "保活方式")
                Card {
                    RadioButtonPreference(
                        title = "前台服务",
                        summary = "默认：常驻通知 + 前台服务",
                        selected = keepAliveMode == KeepAliveMode.FOREGROUND,
                        onClick = { selectKeepAliveMode(KeepAliveMode.FOREGROUND) }
                    )
                    RadioButtonPreference(
                        title = "无障碍保活",
                        summary = if (accEnabled) "已开启：系统守护，被清理后自动拉起"
                        else "未开启：点击跳转系统无障碍设置",
                        selected = keepAliveMode == KeepAliveMode.ACCESSIBILITY,
                        onClick = { selectKeepAliveMode(KeepAliveMode.ACCESSIBILITY) }
                    )
                    RadioButtonPreference(
                        title = "Shizuku 保活",
                        summary = when {
                            shizukuReady -> "已就绪：已写入后台/电池白名单"
                            ShizukuHelper.isBinderReady() -> "未授权：点击授权并应用"
                            else -> "未运行：请先启动 Shizuku"
                        },
                        selected = keepAliveMode == KeepAliveMode.SHIZUKU,
                        onClick = { selectKeepAliveMode(KeepAliveMode.SHIZUKU) },
                        enabled = ShizukuHelper.isBinderReady()
                    )
                    RadioButtonPreference(
                        title = "Root 保活（Magisk）",
                        summary = if (rootAvailable) "可用：点击应用后台/电池白名单"
                        else "未检测到 Root（Magisk）",
                        selected = keepAliveMode == KeepAliveMode.ROOT,
                        onClick = { selectKeepAliveMode(KeepAliveMode.ROOT) },
                        enabled = rootAvailable
                    )
                }

                SmallTitle(text = "系统保活")
                Card {
                    BasicComponent(
                        title = "忽略电池优化",
                        summary = if (ignoringBattery) "已开启：系统不再限制后台运行" else "未开启：点击申请系统白名单",
                        endActions = {
                            Text(
                                text = if (ignoringBattery) "已开启" else "未开启",
                                color = if (ignoringBattery) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        },
                        onClick = { OemUtils.requestIgnoreBatteryOptimizations(context) }
                    )
                    BasicComponent(
                        title = "自启动设置",
                        summary = "跳转 MIUI/HyperOS 等厂商自启动管理",
                        onClick = { OemUtils.openAutoStartSettings(context) }
                    )
                }

                SmallTitle(text = "权限")
                Card {
                    BasicComponent(
                        title = "麦克风",
                        summary = "用于录音触发四扬声器（必要权限）",
                        endActions = {
                            PermissionBadge(micGranted)
                        },
                        onClick = { context.openAppSettings() }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        BasicComponent(
                            title = "通知",
                            summary = "用于后台保活常驻通知（必要权限）",
                            endActions = {
                                PermissionBadge(notifGranted)
                            },
                            onClick = { context.openAppSettings() }
                        )
                    }
                }

                SmallTitle(text = "关于")
                Card {
                    BasicComponent(
                        title = "反馈 & 贡献",
                        summary = "GitHub：github.com/chen2940/keep_4",
                        onClick = { context.openUrl("https://github.com/chen2940/keep_4") }
                    )
                    BasicComponent(
                        title = "免责声明",
                        summary = "查看使用风险与说明",
                        onClick = { showDisclaimer = true }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            OverlayDialog(
                title = "免责声明",
                summary = "本应用仅供联想小新 Pad Pro 12.7（一代）使用，目的是强制开启四扬声器，" +
                    "可能引起耗电增加或系统稳定性问题。使用风险由您自行承担。",
                show = showDisclaimer,
                onDismissRequest = { showDisclaimer = false }
            ) {
                TextButton(
                    text = "知道了",
                    onClick = { showDisclaimer = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OverlayDialog(
                title = "Root 权限警告",
                summary = "Root 权限拥有系统最高控制权，使用不当可能损坏系统、导致数据丢失或安全风险。\n\n" +
                    "本应用仅在你开启四扬保活时，通过 root 写入后台/电池白名单（deviceidle + appops），" +
                    "让保活服务不被系统清理；不会修改系统文件、不收集任何数据。\n\n" +
                    "请确认你已了解并接受上述风险后再开启。",
                show = showRootWarning,
                onDismissRequest = { showRootWarning = false }
            ) {
                TextButton(
                    text = if (rootCountdown > 0) "同意并开启（${rootCountdown}s）" else "同意并开启",
                    onClick = {
                        showRootWarning = false
                        keepAliveMode = KeepAliveMode.ROOT
                        settings.keepAliveMode = KeepAliveMode.ROOT.id
                        applyPrivilegedWhitelist(context, "Root") { RootHelper.runAsRoot(it) }
                    },
                    enabled = rootCountdown <= 0,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    text = "取消",
                    onClick = { showRootWarning = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MiniStatusRow(label: String, value: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (active) MiuixIcons.Basic.Check else MiuixIcons.Basic.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (active) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value,
                style = MiuixTheme.textStyles.body2,
                color = if (active) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }
    }
}

@Composable
private fun PermissionBadge(granted: Boolean) {
    Text(
        text = if (granted) "已授权" else "未授权",
        color = if (granted) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

private fun requiredPermissions(context: Context): List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.startKeep4Service(): Boolean {
    return try {
        startForegroundService(
            Intent(this, SpeakerService::class.java).setAction(SpeakerService.ACTION_START)
        )
        true
    } catch (e: Exception) {
        android.util.Log.e("Keep4", "start service failed", e)
        Toast.makeText(this, "启动保活服务失败：" + e.javaClass.simpleName, Toast.LENGTH_LONG).show()
        false
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.openAppSettings() {
    try {
        startActivity(
            Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    } catch (e: Exception) {
        Toast.makeText(this, "无法打开应用设置", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.openAccessibilitySettings() {
    try {
        startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
    } catch (e: Exception) {
        Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.setExcludeFromRecents(hide: Boolean): Boolean {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val task = am.appTasks.firstOrNull() ?: return false
    // API 37 起签名为 setExcludeFromRecents(boolean)，旧版本为 (Intent)，用反射兼容
    return try {
        val method = task.javaClass.getMethod("setExcludeFromRecents", java.lang.Boolean.TYPE)
        method.invoke(task, hide)
        true
    } catch (e: NoSuchMethodException) {
        try {
            val intent = Intent(this, MainActivity::class.java)
            if (hide) intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            val method = task.javaClass.getMethod("setExcludeFromRecents", Intent::class.java)
            method.invoke(task, intent)
            true
        } catch (e2: Exception) {
            false
        }
    } catch (e: Exception) {
        false
    }
}

/** 通过 Shizuku/Root 写入后台 & 电池白名单，err==null 表示成功 */
private fun applyPrivilegedWhitelist(context: Context, label: String, runner: (String) -> String?) {
    val commands = KeepAlive.whitelistCommands(context.packageName)
    var ok = 0
    val failed = mutableListOf<String>()
    commands.forEach { cmd ->
        val err = runner(cmd)
        if (err == null) {
            ok++
        } else if (err.contains("Unknown operation") || err.contains("No such operation")) {
            // 该 ROM 不支持的 appop，忽略不计入失败
            ok++
        } else {
            failed.add("${cmd.substringAfterLast(' ')}($err)")
        }
    }
    val msg = if (failed.isEmpty()) {
        "$label 保活配置成功（$ok/${commands.size} 项）"
    } else {
        "$label 配置部分失败：${failed.joinToString("; ")}"
    }
    android.util.Log.i("Keep4", "whitelist[$label]: $msg")
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
