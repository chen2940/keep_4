package com.aya.keep4.keepalive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * 厂商系统设置引导（MIUI/HyperOS、EMUI、ColorOS、OriginOS、MagicOS 等），
 * 参考 keep4.1/Keep(Loud) 的 AutoStartUtils。
 */
object OemUtils {

    private const val TAG = "OemUtils"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /** 请求系统"忽略电池优化"白名单（会弹系统授权框） */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) {
            return
        }
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "request battery optimization failed", e)
        }
    }

    /** 打开对应厂商的"自启动管理"设置页 */
    fun openAutoStartSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                tryStart(context, "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") ||
                tryStart(context, "com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity") {
                    putExtra("packageName", context.packageName)
                }
            }
            manufacturer.contains("huawei") -> {
                tryStart(context, "com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity") ||
                tryStart(context, "com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            }
            manufacturer.contains("honor") ->
                tryStart(context, "com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                tryStart(context, "com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity") ||
                tryStart(context, "com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity") ||
                tryStart(context, "com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            }
            manufacturer.contains("vivo") ->
                tryStart(context, "com.vivo.permcenter", "com.vivo.permcenter.autostart.AutoStartManagementActivity")
            else -> openAppDetails(context)
        }
    }

    private fun tryStart(context: Context, pkg: String, cls: String, extra: (Intent.() -> Unit)? = null): Boolean {
        return try {
            val intent = Intent().apply {
                setClassName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                extra?.invoke(this)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "start $pkg/$cls failed", e)
            false
        }
    }

    private fun openAppDetails(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.w(TAG, "open app details failed", e)
        }
    }
}