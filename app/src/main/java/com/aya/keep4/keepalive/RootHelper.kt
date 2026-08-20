package com.aya.keep4.keepalive

/**
 * Root（Magisk）辅助：检测 su 是否可用，并以 root 执行命令。
 * 兼容 Magisk 的 su 交互授权（会弹出 Magisk 授权对话框）。
 */
object RootHelper {

    fun isAvailable(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
        val out = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        p.exitValue() == 0 && out == "0"
    } catch (e: Exception) {
        false
    }

    /** 以 root 执行一段 shell 脚本，成功返回 null，失败返回错误信息 */
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