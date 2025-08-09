package com.example.voicerecorder

object RNNoiseWrapper {
    init {
        try {
            System.loadLibrary("rnnoise")
        } catch (e: UnsatisfiedLinkError) {
            // library not present - denoise will fallback to software methods
        }
    }

    // Native function signatures - require JNI implementation in C/C++
    external fun rnnoiseCreate(): Long
    external fun rnnoiseDestroy(ctx: Long)
    external fun rnnoiseProcessFrame(ctx: Long, input: ShortArray, output: ShortArray): Int
}
