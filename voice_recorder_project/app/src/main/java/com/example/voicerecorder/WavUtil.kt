package com.example.voicerecorder

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

object WavUtil {
    fun writeWaveFile(outFile: File, data: ShortArray, sampleRate: Int, channels: Int) {
        val bos = DataOutputStream(FileOutputStream(outFile))
        val byteRate = 16 * sampleRate * channels / 8
        bos.writeBytes("RIFF")
        bos.writeInt(Integer.reverseBytes(36 + data.size * 2))
        bos.writeBytes("WAVE")
        bos.writeBytes("fmt ")
        bos.writeInt(Integer.reverseBytes(16))
        bos.writeShort(java.lang.Short.reverseBytes(1.toShort()))
        bos.writeShort(java.lang.Short.reverseBytes(channels.toShort()))
        bos.writeInt(Integer.reverseBytes(sampleRate))
        bos.writeInt(Integer.reverseBytes(byteRate))
        bos.writeShort(java.lang.Short.reverseBytes((channels * 16 / 8).toShort()))
        bos.writeShort(java.lang.Short.reverseBytes(16.toShort()))
        bos.writeBytes("data")
        bos.writeInt(Integer.reverseBytes(data.size * 2))
        for (s in data) bos.writeShort(java.lang.Short.reverseBytes(s))
        bos.flush()
        bos.close()
    }
}
