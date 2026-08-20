package com.aya.keep4.keepalive

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.aya.keep4.service.KeepAliveAccessibilityService

/** 保活模式 */
enum class KeepAliveMode(val id: String, val label: String) {
    FOREGROUND("foreground", "前台服务"),
    ACCESSIBILITY("accessibility", "无障碍保活"),
    SHIZUKU("shizuku", "Shizuku 保活"),
    ROOT("root", "Root 保活");

    companion object {
        fun fromId(id: String?): KeepAliveMode =
            entries.firstOrNull { it.id == id } ?: FOREGROUND
    }
}

/** 保活相关工具 */
object KeepAlive {

    /** 通过 shell/root 写入的后台 & 电池白名单命令（Shizuku 与 Root 共用） */
    fun whitelistCommands(packageName: String): List<String> = listOf(
        "dumpsys deviceidle whitelist +$packageName",
        "cmd appops set $packageName RUN_IN_BACKGROUND allow",
        "cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow",

        "cmd appops set $packageName START_FOREGROUND allow",

    )

    fun whitelistScript(packageName: String): String =
        whitelistCommands(packageName).joinToString("; ")

    /** 无障碍服务是否已在系统设置中开启 */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, KeepAliveAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}