package com.prempoddar.loadstone.data.voice

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Short spoken notes for a pin ("third pillar from the gate, blue door"), kept in app storage. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    val recording get() = recorder != null

    fun start(): Boolean {
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val out = File(dir, "note_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(out.path)
            r.prepare()
            r.start()
            recorder = r; file = out
            true
        }.getOrElse { r.release(); out.delete(); false }
    }

    /** The finished note, or null if it was too short to keep or failed. */
    fun stop(): String? {
        val r = recorder ?: return null
        val out = file
        recorder = null; file = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        if (!ok || out == null || durationMs(out.path) < 500) { out?.delete(); return null }
        return out.path
    }

    companion object {
        fun durationMs(path: String): Long = runCatching {
            MediaMetadataRetriever().run {
                try { setDataSource(path); extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L } finally { release() }
            }
        }.getOrDefault(0L)

        fun delete(path: String) { runCatching { File(path).delete() } }
    }
}
