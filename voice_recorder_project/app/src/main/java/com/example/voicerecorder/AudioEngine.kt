package com.example.voicerecorder

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator
import be.tarsos.dsp.io.jvm.WaveformSimilarityBasedOverlapAdd
import be.tarsos.dsp.io.jvm.WaveformSimilarityBasedOverlapAdd.Parameters
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.io.jvm.WaveDecoder
import be.tarsos.dsp.pitch.PitchShifter
import be.tarsos.dsp.writer.WriterProcessor
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.util.fft.FFT
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import javax.sound.sampled.AudioSystem
import kotlin.math.abs

class AudioEngine(private val sampleRate: Int) {
    private var dispatcher: AudioDispatcher? = null
    private val bufferSize = 1024
    private val overlap = 0
    private val recordedFloats = mutableListOf<FloatArray>()

    init {
        // Ensure TarsosDSP's native helpers are available on some platforms
        try {
            AndroidFFMPEGLocator(this::class.java.classLoader).loadFFMPEGIfNeeded()
        } catch (e: Exception) {
            // ignore in Android environment; placeholder for desktop
        }
    }

    fun startRecording(onBuffer: (ShortArray) -> Unit) {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
        dispatcher?.addAudioProcessor(object : AudioProcessor {
            override fun processingFinished() {}
            override fun process(audioEvent: AudioEvent?): Boolean {
                audioEvent ?: return true
                val floatBuf = audioEvent.floatBuffer.clone()
                recordedFloats.add(floatBuf)
                val shortBuf = ShortArray(floatBuf.size)
                for (i in floatBuf.indices) shortBuf[i] = (floatBuf[i] * Short.MAX_VALUE).toInt().toShort()
                onBuffer(shortBuf)
                return true
            }
        })
        Thread { dispatcher?.run() }.start()
    }

    fun stopAndSave(out: File) {
        dispatcher?.stop()
        val total = recordedFloats.sumOf { it.size }
        val shorts = ShortArray(total)
        var pos = 0
        for (chunk in recordedFloats) {
            for (i in chunk.indices) {
                shorts[pos++] = (chunk[i] * Short.MAX_VALUE).toInt().toShort()
            }
        }
        WavUtil.writeWaveFile(out, shorts, sampleRate, 1)
        recordedFloats.clear()
    }

    fun playWithPitch(file: File, pitch: Float) {
        // Read WAV file into shorts
        val shorts = readWavData(file)
        // Naive pitch-shift by resampling (changes duration). For better quality use TarsosDSP PitchShifter.
        val outLength = (shorts.size / pitch).toInt()
        val out = ShortArray(outLength)
        for (i in 0 until outLength) {
            val srcIdx = (i * pitch).toInt()
            out[i] = shorts[srcIdx.coerceAtMost(shorts.size - 1)]
        }
        playShortArray(out)
    }

    fun play(file: File) {
        val shorts = readWavData(file)
        playShortArray(shorts)
    }

    fun playRobot(file: File) {
        val shorts = readWavData(file)
        val out = ShortArray(shorts.size)
        val chunk = 256
        var i = 0
        while (i < shorts.size) {
            val end = (i + chunk).coerceAtMost(shorts.size)
            var avg = 0
            for (j in i until end) avg += abs(shorts[j].toInt())
            avg /= (end - i)
            val v = avg.toShort()
            for (j in i until end) out[j] = v
            i += chunk
        }
        playShortArray(out)
    }

    private fun readWavData(file: File): ShortArray {
        val raf = RandomAccessFile(file, "r")
        // Skip WAV header (44 bytes) - simplistic
        raf.seek(44)
        val remaining = raf.length() - 44
        val shorts = ShortArray((remaining / 2).toInt())
        var idx = 0
        while (raf.filePointer + 1 < raf.length()) {
            val low = raf.readUnsignedByte()
            val high = raf.readByte().toInt()
            val v = (high shl 8) or low
            shorts[idx++] = v.toShort()
        }
        raf.close()
        return shorts
    }

    private fun playShortArray(data: ShortArray) {
        val minBuf = android.media.AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, data.size * 2),
            AudioTrack.MODE_STREAM
        )
        track.play()
        var pos = 0
        val buf = ShortArray(1024)
        while (pos < data.size) {
            val toCopy = kotlin.math.min(buf.size, data.size - pos)
            System.arraycopy(data, pos, buf, 0, toCopy)
            track.write(buf, 0, toCopy)
            pos += toCopy
        }
        track.stop()
        track.release()
    }
}
