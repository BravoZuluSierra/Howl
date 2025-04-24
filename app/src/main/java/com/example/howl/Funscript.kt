package com.example.howl

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.TreeMap
import kotlin.math.pow

class BadFileException (message: String) : Exception(message)

@Serializable
data class Action(val at: Int, val pos: Int)

@Serializable
data class Funscript(
    val actions: List<Action>,
    val howl_max_pos: Int? = null,
    val howl_min_pos: Int? = null,
    val howl_time_offset: Int? = null
)

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

data class PositionVelocity(val position: Double, val velocity: Double)

class FunscriptPulseSource : PulseSource {
    override var displayName: String = "Funscript"
    override var duration: Double? = null
    override val isFinite: Boolean = true
    override val shouldLoop: Boolean = false
    override var readyToPlay: Boolean = false

    private val timePositionData = TreeMap<Double, PositionVelocity>()

    override fun updateState(currentTime: Double) {}

    private fun getClosestPoints(time: Double): Pair<Pair<Double, PositionVelocity>?, Pair<Double, PositionVelocity>?> {
        val floorEntry = timePositionData.floorEntry(time)
        val ceilingEntry = timePositionData.higherEntry(time)
        val before = floorEntry?.let { Pair(it.key, it.value) }
        val after = ceilingEntry?.let { Pair(it.key, it.value) }
        return Pair(before, after)
    }

    fun getPositionAndVelocityAtTime(time: Double): Pair<Double, Double> {
        val (before, after) = getClosestPoints(time)
        return when {
            before == null && after == null -> Pair(0.0, 0.0)
            before == null -> Pair(after!!.second.position, 0.0)
            after == null -> Pair(before.second.position, 0.0)
            else -> {
                val (t0, pv0) = before
                val (t1, pv1) = after
                if (t0 == t1 || pv0.position == pv1.position) {
                    Pair(pv0.position, 0.0)
                } else {
                    val (rawPos, rawVel) = hermiteInterpolateWithVelocity(
                        time, t0, pv0.position, pv0.velocity, t1, pv1.position, pv1.velocity
                    )
                    Pair(rawPos.coerceIn(0.0, 1.0), rawVel)
                }
            }
        }
    }

    override fun getPulseAtTime(time: Double): Pulse {
        val advancedControlState = DataRepository.playerAdvancedControlsState.value
        val funscriptPositionalEffectStrength = advancedControlState.funscriptPositionalEffectStrength
        val funscriptFrequencyTimeOffset = advancedControlState.funscriptFrequencyTimeOffset
        val velocitySensitivity = 1.0f - advancedControlState.funscriptVolume
        val feelExponent = advancedControlState.funscriptFeel

        var (position, velocity) = getPositionAndVelocityAtTime(time)
        var (offsetPosition, _) = getPositionAndVelocityAtTime(time - funscriptFrequencyTimeOffset)

        var amplitude = 0.0
        amplitude = scaleVelocity(velocity, velocitySensitivity.toDouble())

        val (amplitudeA, amplitudeB) = calculatePositionalEffect(amplitude, position, funscriptPositionalEffectStrength.toDouble())

        var freqA = calculateFeelAdjustment(offsetPosition, feelExponent.toDouble())
        var freqB = calculateFeelAdjustment(position, feelExponent.toDouble())

        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = amplitudeA.toFloat(),
            ampB = amplitudeB.toFloat()
        )
    }

    private data class ScaledAction(val time: Double, val pos: Double)

    fun open(uri: Uri, context: Context): Double? {
        readyToPlay = false
        timePositionData.clear()

        val fileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
        val fileSize: Long = fileDescriptor?.statSize ?: 0
        fileDescriptor?.close()

        if (fileSize > 20 * 1024 * 1024) {
            throw BadFileException("File is too large (>${fileSize / (1024 * 1024)}MB)")
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val content = inputStream.bufferedReader().use { it.readText() }
            val jsonConfig = Json { ignoreUnknownKeys = true }
            try {
                val funscript = jsonConfig.decodeFromString<Funscript>(content)
                val positions = funscript.actions.map { it.pos }
                val minPos = funscript.howl_min_pos ?: positions.minOrNull() ?: 0
                val maxPos = funscript.howl_max_pos ?: positions.maxOrNull() ?: 100
                val (scaleFactor, offset) = if (minPos != maxPos) {
                    val range = (maxPos - minPos).toDouble()
                    Pair(1.0 / range, -minPos.toDouble())
                } else {
                    Pair(1.0 / 100.0, 0.0)
                }

                val scaledActions = funscript.actions.map { action ->
                    val time = action.at / 1000.0
                    val scaledPos = ((action.pos.toDouble() + offset) * scaleFactor).coerceIn(0.0, 1.0)
                    ScaledAction(time, scaledPos)
                }

                val positionVelocities = scaledActions.mapIndexed { i, current ->
                    val prev = if (i > 0) scaledActions[i - 1] else null
                    val next = if (i < scaledActions.size - 1) scaledActions[i + 1] else null

                    val velocity =  when {
                        prev == null && next == null -> 0.0
                        prev == null -> (next!!.pos - current.pos) / (next.time - current.time)
                        next == null -> (current.pos - prev.pos) / (current.time - prev.time)
                        else -> {
                            val slopePrev = (current.pos - prev.pos) / (current.time - prev.time)
                            val slopeNext = (next.pos - current.pos) / (next.time - current.time)
                            ((slopePrev + slopeNext) * 0.5)
                        }
                    }
                    PositionVelocity(current.pos, velocity)
                }

                scaledActions.forEachIndexed { index, action ->
                    timePositionData[action.time] = positionVelocities[index]
                }

            } catch (_: SerializationException) {
                throw BadFileException("Funscript decoding failed")
            }
        }

        displayName = uri.getName(context)
        duration = timePositionData.lastKey()
        readyToPlay = true
        return duration
    }
}