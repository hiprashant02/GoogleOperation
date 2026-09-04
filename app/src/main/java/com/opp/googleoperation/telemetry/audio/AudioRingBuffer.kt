package com.opp.googleoperation.telemetry.audio

class AudioRingBuffer(durationSeconds: Int = 15, sampleRate: Int = 16000) {

    // 16kHz, 16-bit mono = 32000 bytes per second
    private val capacity = sampleRate * 2 * durationSeconds
    private val buffer = ByteArray(capacity)
    private var writeHead = 0
    private var isFilled = false
    private val lock = Any()

    fun write(data: ByteArray, offset: Int, length: Int) {
        synchronized(lock) {
            var srcOffset = offset
            var remaining = length

            while (remaining > 0) {
                val spaceToEnd = capacity - writeHead
                val toWrite = minOf(remaining, spaceToEnd)
                System.arraycopy(data, srcOffset, buffer, writeHead, toWrite)

                writeHead += toWrite
                srcOffset += toWrite
                remaining -= toWrite

                if (writeHead >= capacity) {
                    writeHead = 0
                    isFilled = true
                }
            }
        }
    }

    fun getSnapshot(): ByteArray {
        synchronized(lock) {
            val totalBytes = if (isFilled) capacity else writeHead
            val out = ByteArray(totalBytes)

            if (!isFilled) {
                System.arraycopy(buffer, 0, out, 0, writeHead)
            } else {
                val tailSize = capacity - writeHead
                System.arraycopy(buffer, writeHead, out, 0, tailSize)
                System.arraycopy(buffer, 0, out, tailSize, writeHead)
            }
            return out
        }
    }

    fun clear() {
        synchronized(lock) {
            writeHead = 0
            isFilled = false
        }
    }
}
