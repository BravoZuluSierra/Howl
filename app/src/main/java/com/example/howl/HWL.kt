package com.example.howl

import android.content.Context
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HWLPulseSource : PulseSource {
    override var displayName: String = "HWL"
    override var duration: Double? = null
    override val isFinite: Boolean = true
    override val shouldLoop: Boolean = true
    override var readyToPlay: Boolean = false

    var pulseData: MutableList<Pulse> = mutableListOf<Pulse>()

    override fun updateState(currentTime: Double) {}

    override fun getPulseAtTime(time: Double): Pulse {
        // Calculate the index of the pulse based on the time
        val index = (time / DGCoyote.PULSE_TIME).toInt()

        // Ensure the index is within the bounds of the pulseData list
        if (index < 0) {
            return pulseData.firstOrNull() ?: Pulse()
        }
        if (index >= pulseData.size) {
            return pulseData.lastOrNull() ?: Pulse()
        }

        return pulseData[index]
    }

    fun open(uri: Uri, context: Context): Double? {
        readyToPlay = false
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            // Check the HWL file has the correct header
            val header = ByteArray(8)
            val bytesRead = inputStream.read(header)
            if (bytesRead != 8 || String(header) != "YEAHBOI!") {
                throw BadFileException("Invalid HWL file: expected header not found.")
            }

            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            while (buffer.hasRemaining()) {
                if (buffer.remaining() < 16) {
                    throw BadFileException("Invalid HWL file: incomplete Pulse data.")
                }
                val ampA = buffer.float
                val ampB = buffer.float
                val freqA = buffer.float
                val freqB = buffer.float
                pulseData.add(Pulse(ampA, ampB, freqA, freqB))
            }
        }

        displayName = uri.getName(context)
        duration = pulseData.size * DGCoyote.PULSE_TIME
        readyToPlay = true
        return duration
    }
}