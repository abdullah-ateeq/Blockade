package com.example.blockade.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class GameSound { SELECT, MOVE, BARRIER, INVALID, WIN }

/**
 * Subtle procedurally synthesised effects. The short tones are rendered to WAV files in the cache
 * directory once, then played through SoundPool for low latency. No audio assets are shipped.
 */
class SoundPlayer(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = ConcurrentHashMap<GameSound, Int>()

    init {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        thread(name = "blockade-sfx") {
            GameSound.entries.forEach { sound ->
                val file = File(dir, "${sound.name.lowercase()}_v1.wav")
                if (!file.exists()) writeWav(file, synthesize(sound))
                soundIds[sound] = pool.load(file.absolutePath, 1)
            }
        }
    }

    fun play(sound: GameSound) {
        val id = soundIds[sound] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    private fun synthesize(sound: GameSound): FloatArray = when (sound) {
        GameSound.SELECT -> tone(freq = 1320.0, durationMs = 45, decay = 70.0, gain = 0.18)
        GameSound.MOVE -> mix(
            tone(freq = 520.0, durationMs = 90, decay = 45.0, gain = 0.32, pitchDrop = 0.12),
            tone(freq = 1040.0, durationMs = 50, decay = 80.0, gain = 0.08)
        )
        GameSound.BARRIER -> mix(
            tone(freq = 196.0, durationMs = 160, decay = 26.0, gain = 0.42, pitchDrop = 0.08),
            tone(freq = 392.0, durationMs = 110, decay = 40.0, gain = 0.14),
            tone(freq = 2400.0, durationMs = 18, decay = 220.0, gain = 0.05)
        )
        GameSound.INVALID -> concat(
            tone(freq = 174.0, durationMs = 70, decay = 30.0, gain = 0.28, harmonics = true),
            silence(35),
            tone(freq = 164.0, durationMs = 90, decay = 30.0, gain = 0.28, harmonics = true)
        )
        GameSound.WIN -> {
            val notes = listOf(523.25, 659.25, 783.99, 1046.5)
            val step = 95
            val total = step * (notes.size - 1) + 520
            val out = FloatArray(samples(total))
            notes.forEachIndexed { i, f ->
                val t = tone(freq = f, durationMs = 520, decay = 7.0, gain = 0.2)
                val offset = samples(step * i)
                for (k in t.indices) if (offset + k < out.size) out[offset + k] += t[k]
            }
            out
        }
    }

    private fun samples(ms: Int) = SAMPLE_RATE * ms / 1000

    private fun silence(ms: Int) = FloatArray(samples(ms))

    private fun tone(
        freq: Double,
        durationMs: Int,
        decay: Double,
        gain: Double,
        pitchDrop: Double = 0.0,
        harmonics: Boolean = false
    ): FloatArray {
        val n = samples(durationMs)
        val out = FloatArray(n)
        var phase = 0.0
        val attack = samples(4).coerceAtLeast(1)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val f = freq * (1.0 - pitchDrop * (i.toDouble() / n))
            phase += 2 * PI * f / SAMPLE_RATE
            var v = sin(phase)
            if (harmonics) v = 0.7 * v + 0.2 * sin(2 * phase) + 0.1 * sin(3 * phase)
            val env = exp(-decay * t) * (if (i < attack) i.toDouble() / attack else 1.0)
            val release = if (i > n - attack) (n - i).toDouble() / attack else 1.0
            out[i] = (v * env * release * gain).toFloat()
        }
        return out
    }

    private fun mix(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.maxOf { it.size })
        parts.forEach { p -> p.forEachIndexed { i, v -> out[i] += v } }
        return out
    }

    private fun concat(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.sumOf { it.size })
        var offset = 0
        parts.forEach { p -> p.copyInto(out, offset); offset += p.size }
        return out
    }

    private fun writeWav(file: File, data: FloatArray) {
        val pcmBytes = data.size * 2
        val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(pcmBytes)
        data.forEach { buffer.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
        FileOutputStream(file).use { it.write(buffer.array()) }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
