package com.example.howl

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class WavePoint(
    val time: Double,
    val position: Double,
    val slope: Double
)

enum class InterpolationType {
    HERMITE, LINEAR
}

data class WaveShape(
    val name: String,
    val points: List<WavePoint>,
    val interpolationType: InterpolationType = InterpolationType.HERMITE
)

data class Quadruple<T1, T2, T3, T4>(
    val first: T1,
    val second: T2,
    val third: T3,
    val fourth: T4
)

fun Float.roughlyEqual(other: Float) : Boolean {
    return (abs(this-other) < 1e-6)
}

fun Double.roughlyEqual(other: Double) : Boolean {
    return (abs(this-other) < 1e-6)
}

fun Double.scaleBetween(a: Double, b: Double): Double {
    return (a + (b - a) * this).coerceIn(minOf(a, b), maxOf(a, b))
}

val ClosedRange<Double>.toFloatRange: ClosedFloatingPointRange<Float>
    get() = this.start.toFloat()..this.endInclusive.toFloat()

fun randomInRange(range: ClosedRange<Double>): Double {
    return Random.nextDouble(range.start, range.endInclusive)
}

fun randomInRange(range: IntRange): Int {
    return range.random()
}

inline fun <reified T: Enum<T>> T.next(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}

fun scaleVelocity(velocity: Double, sensitivity: Double): Double {
    val scaledVelocity = if (velocity != 0.0) {
        abs(velocity) / (abs(velocity) + sensitivity)
    } else 0.0
    return scaledVelocity
}

fun smoothstep(t: Double): Double {
    val clamped = t.coerceIn(0.0..1.0)
    return clamped * clamped * (3.0 - 2.0 * clamped)
}

fun linearInterpolate(
    t: Double,
    t0: Double,
    p0: Double,
    t1: Double,
    p1: Double
): Double {
    if (t0 >= t1) return p0
    val h = (t - t0) / (t1 - t0)
    return p0 + h * (p1 - p0)
}

fun linearInterpolateWithVelocity(
    t: Double,
    t0: Double,
    p0: Double,
    t1: Double,
    p1: Double
): Pair<Double, Double> {
    if (t0 >= t1) return Pair(p0, 0.0)
    val h = (t - t0) / (t1 - t0)
    val position = p0 + h * (p1 - p0)
    val velocity = (p1 - p0) / (t1 - t0)
    return Pair(position, velocity)
}

private fun getHermitePositionAndFactors(
    t: Double,
    t0: Double,
    p0: Double,
    m0: Double,
    t1: Double,
    p1: Double,
    m1: Double
): Quadruple<Double, Double, Double, Double> {
    if (t0 >= t1) {
        return Quadruple(p0, 0.0, 0.0, 0.0)
    }
    val h = (t - t0) / (t1 - t0)
    val hSq = h * h
    val hCu = hSq * h

    val position = p0 * (2 * hCu - 3 * hSq + 1) +
            m0 * (hCu - 2 * hSq + h) * (t1 - t0) +
            p1 * (-2 * hCu + 3 * hSq) +
            m1 * (hCu - hSq) * (t1 - t0)

    return Quadruple(position, h, hSq, hCu)
}

fun hermiteInterpolate(
    t: Double,
    t0: Double,
    p0: Double,
    m0: Double,
    t1: Double,
    p1: Double,
    m1: Double
): Double {
    return getHermitePositionAndFactors(t, t0, p0, m0, t1, p1, m1).first
}

fun hermiteInterpolateWithVelocity(
    t: Double,
    t0: Double,
    p0: Double,
    m0: Double,
    t1: Double,
    p1: Double,
    m1: Double
): Pair<Double, Double> {
    val (position, h, hSq, hCu) = getHermitePositionAndFactors(t, t0, p0, m0, t1, p1, m1)

    val dpdh = (6 * hSq - 6 * h) * p0 +
            (3 * hSq - 4 * h + 1) * m0 * (t1 - t0) +
            (-6 * hSq + 6 * h) * p1 +
            (3 * hSq - 2 * h) * m1 * (t1 - t0)
    val velocity = if (t1 != t0) dpdh / (t1 - t0) else 0.0

    return Pair(position, velocity)
}

fun calculatePositionalEffect(
    amplitude: Double,
    position: Double,
    positionalEffectStrength: Double
): Pair<Double, Double> {
    //calculate the "effective position" by interpolating between the original position and the neutral position based on the effect strength
    val effectivePosition = 0.5 * (1 - positionalEffectStrength) + position * positionalEffectStrength

    val amplitudeA = amplitude * sqrt(1 - effectivePosition)
    val amplitudeB = amplitude * sqrt(effectivePosition)
    return Pair(amplitudeA, amplitudeB)
}

fun calculateFeelAdjustment(
    frequency: Double,
    feelExponent: Double,
): Double {
    return frequency.pow(feelExponent).coerceIn(0.0,1.0)
}

class TimerManager {
    private val timers = mutableMapOf<String, Timer>()

    fun addTimer(key: String, duration: Double, callback: () -> Unit) {
        if (duration <= 0) {
            callback()
            return
        }
        timers[key] = Timer(duration, duration, callback)
    }

    fun hasTimer(key: String): Boolean = timers.containsKey(key)

    fun getRemainingTime(key: String): Double? {
        return timers[key]?.remainingTime
    }

    fun getElapsedTime(key: String): Double? {
        val timer = timers[key] ?: return null
        return timer.initialDuration - timer.remainingTime
    }

    fun getProportionElapsed(key: String): Double? {
        val timer = timers[key] ?: return null
        return (timer.initialDuration - timer.remainingTime) / timer.initialDuration
    }

    fun update(delta: Double) {
        require(delta >= 0.0) { "Time delta may not be negative"}
        // Create a snapshot of keys to avoid concurrent modification
        val keysSnapshot = timers.keys.toList()

        keysSnapshot.forEach { key ->
            timers[key]?.let { timer ->
                timer.remainingTime -= delta
                if (timer.remainingTime <= 0) {
                    // Remove before callback to prevent interference
                    timers.remove(key)
                    timer.callback()
                }
            }
        }
    }

    private data class Timer(
        val initialDuration: Double,
        var remainingTime: Double,
        val callback: () -> Unit
    )
}

class SmoothedValue(initialValue: Double = 0.0) {
    //implements a value that is capable of smoothly transitioning towards a target over time
    private var start: Double = initialValue
    private var target: Double = initialValue
    private var elapsed: Double = 0.0
    private var duration: Double = 0.0
    private var onReached: (() -> Unit)? = null

    val current: Double
        get() = if (!isTransitioning) target else calculateInterpolatedValue()

    val isTransitioning: Boolean
        get() = elapsed < duration && duration > 0.0

    fun setTarget(
        target: Double,
        rate: Double? = null,
        duration: Double? = null,
        onReached: (() -> Unit)? = null
    ) {
        require((rate == null) xor (duration == null)) {
            "Must specify exactly one of rate or duration"
        }

        start = current
        this.target = target
        val difference = abs(target - start)

        this.duration = when {
            rate != null -> if (rate > 0.0) difference / rate else 0.0
            duration != null -> if (difference > 0.0) duration else 0.0
            else -> 0.0 // Impossible due to require check
        }

        elapsed = 0.0
        this.onReached = onReached

        if (!isTransitioning) {
            handleTransitionComplete()
        }
    }

    fun getTarget(): Double {
        return target
    }

    fun setImmediately(value: Double) {
        start = value
        target = value
        elapsed = 0.0
        duration = 0.0
        onReached = null
    }

    fun update(delta: Double) {
        require(delta >= 0.0) { "Time delta may not be negative"}
        if (!isTransitioning) return

        elapsed += delta
        if (!isTransitioning) {
            handleTransitionComplete()
        }
    }

    private fun calculateInterpolatedValue(): Double {
        val t = (elapsed / duration).coerceIn(0.0..1.0)
        return start + (target - start) * smoothstep(t)
    }

    private fun handleTransitionComplete() {
        val callback = onReached
        onReached = null
        callback?.invoke()
    }
}