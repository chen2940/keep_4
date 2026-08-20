package com.aya.keep4.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aya.keep4.data.AppState
import kotlin.concurrent.thread

/**
 * 四扬触发器。
 *
 * - [Strategy.AUDIO_RECORD]：用 VOICE_COMMUNICATION 音源开一个 AudioRecord，读到的数据直接丢弃，
 *   把系统顶进"通话/录音"音频路由，从而驱动四个扬声器（不再写 3gp 文件）。
 * - [Strategy.COMMUNICATION_MODE]：不占用麦克风，切入 MODE_IN_COMMUNICATION + 免提路由，
 *   部分 ROM 可能无效（可在界面中切换实测）。
 */
class QuadSpeakerTrigger(private val context: Context) {

    enum class Strategy { AUDIO_RECORD, COMMUNICATION_MODE }

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var recording = false
    private var audioRecord: AudioRecord? = null
    private var readThread: Thread? = null
    private var prevMode = AudioManager.MODE_NORMAL

    private val handler = Handler(Looper.getMainLooper())

    /** MIUI/HyperOS 可能自动重置音频模式，周期性重新断言免提路由 */
    private val modeKeeper = object : Runnable {
        override fun run() {
            try {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
            } catch (e: Exception) {
                Log.w(TAG, "keep communication mode failed", e)
            }
            handler.postDelayed(this, MODE_KEEP_INTERVAL_MS)
        }
    }

    @Synchronized
    fun start(strategy: Strategy) {
        stopInternal()
        when (strategy) {
            Strategy.AUDIO_RECORD -> startRecording()
            Strategy.COMMUNICATION_MODE -> startCommunicationMode()
        }
    }

    @Synchronized
    fun stop() = stopInternal()

    private fun startRecording() {
        try {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return
            val record = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf * 2, 4096))
                .build()
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return
            }
            audioRecord = record
            record.startRecording()
            recording = true
            AppState.triggerActive.value = true
            readThread = thread(name = "keep4-discard") {
                val buf = ShortArray(4096)
                while (recording) {
                    try {
                        val n = record.read(buf, 0, buf.size)
                        if (n <= 0) Thread.sleep(10)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "startRecording failed", e)
            stopInternal()
        }
    }

    private fun startCommunicationMode() {
        try {
            prevMode = audioManager.mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            AppState.triggerActive.value = true
            // 周期性重新断言，避免被系统（尤其 MIUI/HyperOS）自动重置
            handler.removeCallbacks(modeKeeper)
            handler.post(modeKeeper)
        } catch (e: Exception) {
            Log.w(TAG, "startCommunicationMode failed", e)
        }
    }

    private fun stopInternal() {
        handler.removeCallbacks(modeKeeper)
        recording = false
        val record = audioRecord
        audioRecord = null
        if (record != null) {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "stop record failed", e)
            }
            try {
                readThread?.join(500)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            readThread = null
            try {
                record.release()
            } catch (e: Exception) {
                Log.w(TAG, "release record failed", e)
            }
        }
        if (prevMode != AudioManager.MODE_NORMAL) {
            try {
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = prevMode
            } catch (e: Exception) {
                Log.w(TAG, "restore mode failed", e)
            }
            prevMode = AudioManager.MODE_NORMAL
        }
        AppState.triggerActive.value = false
    }

    companion object {
        private const val TAG = "QuadSpeakerTrigger"
        private const val MODE_KEEP_INTERVAL_MS = 1000L
    }
}
