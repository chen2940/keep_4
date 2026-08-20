package com.aya.keep4.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** MiUIx 主题，跟随系统深色模式。 */
@Composable
fun Keep4Theme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller) {
        // MiUIx 的 OverlayDialog 等组件依赖 NavigationEventDispatcher，需在根组合提供
        val dispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
            content()
        }
    }
}