package com.aya.keep4.keepalive

import java.io.File

/**
 * Root（Magisk/KernelSU）辅助。
 *
 * 注意：检测可用性使用【被动检测】——只检查 su/magisk 二进制是否存在，
 * 绝不执行 su，避免打开应用/设置页时触发 root 管理器授权弹窗。
 * 真正的 su 调用只在用户点击开启 Root 模式并确认后发生。
 */
object RootHelper {

    /** 被动检测系统是否存在 root 环境（不弹授权框） */
    fun isAvailable(): Boolean {
        val known = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/system/bin/magisk", "/sbin/magisk",
            "/data/adb/magisk/magisk", "/data/adb/ksu/bin/ksud"
        )
        if (known.any { File(it).exists() }) return true
        val path = System.getenv("PATH") ?: return false
        return path.split(':').any { dir ->
            val d = File(dir)
            File(d, "su").exists() || File(d, "magisk").exists() || File(d, "ksud").exists()
        }
    }

    /** 以 root 执行一段 shell 脚本，成功返回 null，失败返回错误信息（此时才会触发 root 管理器授权） */
    fun runAsRoot(script: String): String? = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor()
        if (p.exitValue() == 0) null else (out + err).ifBlank { "exit ${p.exitValue()}" }
    } catch (e: Exception) {
        e.message
    }
}