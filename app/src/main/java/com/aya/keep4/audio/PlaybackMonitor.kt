package com.aya.keep4.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.aya.keep4.data.AppState

/**
 * 监听系统媒体播放状态（跨应用）。
 *
 * API 26+ 通过 AudioPlaybackCallback 在播放状态变化时被唤醒，再查询 isMusicActive
 * 判定当前是否有媒体在播放（API 37 起 AudioPlaybackConfiguration.isActive() 已不在公共 API）。
 * 另保留 1s 轮询（部分 ROM 的回调不实时），保证播放状态 1 秒内反馈到界面。仅在状态翻转时回调外部。
 */
class PlaybackMonitor(
    private val context: Context,
    private val onStateChanged: (active: Boolean) -> Unit
) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastActive = false

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            update(audioManager.isMusicActive)
        }
    }

    private val poller = object : Runnable {
        override fun run() {
            update(audioManager.isMusicActive)
            handler.postDelayed(this, SAFETY_POLL_INTERVAL_MS)
        }
    }

    val isPlaybackActive: Boolean get() = lastActive

    fun start() {
        lastActive = audioManager.isMusicActive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.registerAudioPlaybackCallback(callback, handler)
        }
        handler.post(poller)
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.unregisterAudioPlaybackCallback(callback)
        }
        handler.removeCallbacks(poller)
    }

    private fun update(active: Boolean) {
        if (active != lastActive) {
            lastActive = active
            AppState.playbackActive.value = active
            onStateChanged(active)
        }
    }

    companion object {
        private const val SAFETY_POLL_INTERVAL_MS = 1000L
    }
}