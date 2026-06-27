package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

object RetroSoundGenerator {
    private const val SAMPLE_RATE = 22050

    // Synthesizes an arpeggio sound (Level Up)
    suspend fun playLevelUp() = withContext(Dispatchers.Default) {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
        val durationPerNote = 0.15f // seconds
        val totalSamples = (SAMPLE_RATE * durationPerNote * notes.size).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val noteIndex = (i / (SAMPLE_RATE * durationPerNote)).toInt().coerceIn(0, notes.lastIndex)
            val freq = notes[noteIndex]
            val time = i.toDouble() / SAMPLE_RATE
            // Square wave with a gentle decay filter
            val signal = if ((time * freq) % 1.0 < 0.5) 1.0 else -1.0
            val volumeCoeff = 1.0 - (i.toDouble() / totalSamples) * 0.4
            buffer[i] = (signal * 4000 * volumeCoeff).toInt().toShort()
        }
        playRawAudio(buffer)
    }

    // Synthesizes a damage sound (Hit)
    suspend fun playDamage() = withContext(Dispatchers.Default) {
        val duration = 0.2f // seconds
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Noise mixed with low square wave
            val noise = Random.nextDouble(-1.0, 1.0)
            val freq = 120.0 * (1.0 - progress) // Pitch descent
            val t = i.toDouble() / SAMPLE_RATE
            val square = if ((t * freq) % 1.0 < 0.5) 1.0 else -1.0
            val mixed = (noise * 0.7 + square * 0.3)
            val envelope = (1.0 - progress) // Simple linear decay
            buffer[i] = (mixed * envelope * 8000).toInt().toShort()
        }
        playRawAudio(buffer)
    }

    // Synthesizes a dice roll (Clink/Click)
    suspend fun playDiceRoll() = withContext(Dispatchers.Default) {
        val duration = 0.4f // seconds
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Rapid clicking sounds using high-frequency sine waves with localized bursts
            val clickOn = sin(progress * 100.0) > 0.8
            val noise = if (clickOn) Random.nextDouble(-1.0, 1.0) * 0.5 else 0.0
            val envelope = 1.0 - progress
            buffer[i] = (noise * envelope * 6000).toInt().toShort()
        }
        playRawAudio(buffer)
    }

    // Synthesizes a Rest/Heal sound (Ascending chime)
    suspend fun playHeal() = withContext(Dispatchers.Default) {
        val duration = 0.5f // seconds
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Ascending swept sine wave for magical spell vibe
            val startFreq = 440.0
            val endFreq = 880.0
            val freq = startFreq + (endFreq - startFreq) * progress
            val phase = 2.0 * Math.PI * freq * (progress * duration) // Quadratic phase for glide
            val value = sin(phase)
            val envelope = sin(progress * Math.PI) * 0.8 // Fade in and out smoothly
            buffer[i] = (value * envelope * 10000).toInt().toShort()
        }
        playRawAudio(buffer)
    }

    private fun playRawAudio(buffer: ShortArray) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Release after playing is finished
            val durationMs = (buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 50
            Thread.sleep(durationMs)
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
