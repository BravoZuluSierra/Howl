package com.example.howl

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TreeMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class BadFileException (message: String) : Exception(message)

fun Uri.getName(context: Context): String {
    val returnCursor = context.contentResolver.query(this, null, null, null, null)
    if (returnCursor == null)
        return ""
    val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    returnCursor.moveToFirst()
    val fileName = returnCursor.getString(nameIndex)
    returnCursor.close()
    return fileName
}

open class FileReader {
    var readyToPlay: Boolean = false
    var filename = ""

    open fun open(uri: Uri, context: Context): Double {
        // The overriding class should read in the file data and do any initial processing required
        // Return the length of the file in seconds
        filename = uri.getName(context)
        return 0.0
    }
    open fun getPulseAtTime(time: Double): Pulse {
        // The overriding class should return the Pulse that we need to send to the Coyote
        // based on the time in seconds passed to the function
        return Pulse()
    }
}

class HWLReader: FileReader() {
    var pulseData: MutableList<Pulse> = mutableListOf<Pulse>()

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

    override fun open(uri: Uri, context: Context): Double {
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
        super.open(uri, context)
        readyToPlay = true
        return pulseData.size * DGCoyote.PULSE_TIME
    }
}

class FunscriptReader: FileReader() {
    private val timePositionData = TreeMap<Double, Double>()

    @Serializable
    data class Action(val at: Int, val pos: Int)

    @Serializable
    data class Funscript(val actions: List<Action>)

    private fun addTimePosition(time: Double, position: Double) {
        // Adds a point in the funscript to our TreeMap
        timePositionData[time] = position
    }

    private fun getClosestPoints(time: Double): Pair<Pair<Double, Double>?, Pair<Double, Double>?> {
        // Return the closest two points in our TreeMap that the given time falls between
        val floorEntry = timePositionData.floorEntry(time)
        val ceilingEntry = timePositionData.higherEntry(time)

        val before = floorEntry?.let { Pair(it.key, it.value) }
        val after = ceilingEntry?.let { Pair(it.key, it.value) }

        return Pair(before, after)
    }

    fun getPositionAndVelocityAtTime(time: Double): Pair<Double, Double> {
        // Calculate the position and velocity of the "stroker" at any given time
        // Ignores any mechanical factors like motor acceleration or inertia
        val (before, after) = getClosestPoints(time)
        return when {
            before == null && after == null -> Pair(0.0, 0.0) // No data points
            before == null -> Pair(after!!.second, 0.0) // Time is before the first point
            after == null -> Pair(before.second, 0.0) // Time is after the last point
            else -> {
                val (t1, p1) = before
                val (t2, p2) = after
                val position = p1 + (p2 - p1) * (time - t1) / (t2 - t1)
                val velocity = (p2 - p1) / (t2 - t1)
                Pair(position, velocity)
            }
        }
    }

    override fun getPulseAtTime(time: Double): Pulse {
        val velocityCorrection = 0.2
        val minIntensity = 0.4
        val movingThreshold = 0.01
        val advancedControlState = DataRepository.funscriptAdvancedControlsState.value
        val channelBiasFactor = advancedControlState.channelBiasFactor

        var (position, velocity) = getPositionAndVelocityAtTime(time)

        // Calculate the base amplitude depending on what the velocity of the "stroker" would be
        // Map it into the range between minIntensity and 1 since very weak signals aren't noticeable on estim
        // velocityCorrection=0.2 roughly equates to setting amplitude 1 when the script is at the maximum speed
        // of a device like "The Handy" (about 5 full range strokes per second).
        var amplitude = 0.0
        if(abs(velocity) > movingThreshold) {
            amplitude = min(1.0, abs(velocity * velocityCorrection))
            amplitude = amplitude * (1 - minIntensity) + minIntensity
        }

        // Calculate the amplitude of the individual channels A and B such that the electrode closer to the
        // "stroker head" position is stronger and the other is weaker. "channelBiasFactor" determines the strength
        // of this effect (at 0 both channels are the same amplitude, at 1 a signal at the very top or bottom of the
        // range only plays on one channel). Set up correctly there should be a "panning" feeling of the signal
        // moving between the electrodes as the funscript position changes.
        val normalisedPosition = 2.0 * position - 1 //between -1 and 1
        val rawAmplitudeA = 1.0 - normalisedPosition * channelBiasFactor
        val rawAmplitudeB = 1.0 + normalisedPosition * channelBiasFactor

        // "normalisationFactor" is an attempt to moderate the perceived intensity of the signals so that positions
        // in the middle of the range where both channels are playing evenly don't always feel stronger than
        // positions at the extremes. It assumes that the perceived intensity of two signals is proportional to the sum
        // of the squares of their amplitudes (similar to a technique sometimes used for audio panning).
        val normalisationFactor = 1.0 / sqrt(rawAmplitudeA * rawAmplitudeA + rawAmplitudeB * rawAmplitudeB)
        val amplitudeA = rawAmplitudeA * normalisationFactor * amplitude
        val amplitudeB = rawAmplitudeB * normalisationFactor * amplitude

        // The frequencies used are simply based on the the position, so the top "stroker head" position
        // corresponds to the maximum frequency. This can be further modulated or inverted by the player settings.
        return Pulse(freqA = position.toFloat(), freqB = position.toFloat(), ampA = amplitudeA.toFloat(), ampB = amplitudeB.toFloat())
    }

    override fun open(uri: Uri, context: Context): Double {
        readyToPlay = false
        timePositionData.clear()

        // Get the file size
        val fileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
        val fileSize: Long = fileDescriptor?.statSize ?: 0
        fileDescriptor?.close()

        // Check if the file size is larger than 20MB
        // Avoids an OOM error with the JSON decoder if the user opens any very large file
        if (fileSize > 20 * 1024 * 1024) {
            throw BadFileException("File is too large (>${fileSize / (1024 * 1024)}MB)")
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            // Read in and decode the funscript JSON
            val content = inputStream.bufferedReader().use { it.readText() }
            val jsonConfig = Json { ignoreUnknownKeys = true }
            try {
                val funscript = jsonConfig.decodeFromString<Funscript>(content)
                // Add each point in the funscript to our TreeMap
                // Convert to our internal format with times in seconds and positions between 0 and 1
                funscript.actions.forEach {
                    addTimePosition(it.at/1000.0,it.pos/100.0)
                }
            }
            catch (_: SerializationException) {
                throw BadFileException("Funscript decoding failed")
            }
        }
        super.open(uri, context)
        readyToPlay = true
        return timePositionData.lastKey()
    }
}