package com.aya.keep4.keepalive

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 辅助：通过 Shizuku 以 shell 权限执行命令（无需 root）。
 * 需要设备安装 Shizuku 应用并通过 adb/root 启动。
 */
object ShizukuHelper {

    /** Shizuku 服务是否连接（binder 可用） */
    fun isBinderReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    /** 是否已获得 Shizuku 授权 */
    fun isPermissionGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    fun isReady(): Boolean = isBinderReady() && isPermissionGranted()

    /** 请求授权（会弹出 Shizuku 授权对话框，结果通过监听器回调） */
    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Throwable) {
            // 忽略：未安装 Shizuku 或无授权入口
        }
    }

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (e: Throwable) {
            // 忽略
        }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (e: Throwable) {
            // 忽略
        }
    }

    /** 通过 Shizuku 执行 shell 脚本，成功返回 null，失败返回错误信息 */
    fun run(script: String): String? = try {
        val p = Shizuku.newProcess(arrayOf("sh", "-c", script), null, null)
            ?: return "Shizuku binder not ready"
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        val code = p.waitFor()
        if (code == 0) null else (out + err).ifBlank { "exit $code" }
    } catch (e: Throwable) {
        e.message
    }
}