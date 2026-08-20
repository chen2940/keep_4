package com.aya.keep4.data

import android.content.Context

/** 开关与策略的本地持久化（替代原 WebView localStorage）。 */
class SettingsStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("keep4_settings", Context.MODE_PRIVATE)

    /** 四扬声器总开关 */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** 免录音模式（通信模式路由触发，不占用麦克风） */
    var communicationMode: Boolean
        get() = prefs.getBoolean(KEY_COMM_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_COMM_MODE, value).apply()

    /** 从最近任务中隐藏 */
    var hideFromRecents: Boolean
        get() = prefs.getBoolean(KEY_HIDE_RECENTS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_RECENTS, value).apply()



    /** 保活模式：foreground / accessibility / shizuku / root */
    var keepAliveMode: String
        get() = prefs.getString(KEY_KEEP_ALIVE_MODE, "foreground") ?: "foreground"
        set(value) = prefs.edit().putString(KEY_KEEP_ALIVE_MODE, value).apply()

    /** 是否已做过首次运行权限引导 */
    var permissionsPrompted: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS_PROMPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS_PROMPTED, value).apply()

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_COMM_MODE = "communication_mode"
        private const val KEY_HIDE_RECENTS = "hide_from_recents"
        private const val KEY_PERMISSIONS_PROMPTED = "permissions_prompted"
        private const val KEY_KEEP_ALIVE_MODE = "keep_alive_mode"
    }
}
