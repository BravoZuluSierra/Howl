package com.example.howl

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

sealed class Activity {
    val timerManager = TimerManager()

    abstract val displayName: String
    abstract val iconResId: Int

    open fun runSimulation(deltaSimulationTime: Double) {
        timerManager.update(deltaSimulationTime)
    }
    abstract fun getPulse(): Pulse
}

class LickActivity : Activity() {
    override val displayName = "Infinite licks"
    override val iconResId = R.drawable.grin_tongue
    enum class LickType {
        UNIDIRECTIONAL,
        BIDIRECTIONAL
    }

    var waveManager: WaveManager = WaveManager()
    var lickStartPoint = 0.0
    var lickEndPoint = 1.0
    var lickType = LickType.BIDIRECTIONAL


    init {
        val bidirectional = CyclicalWave(
            WaveShape(
                name = "bidirectional",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(0.5, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        val unidirectional = CyclicalWave(
            WaveShape(
                name = "unidirectional",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(1.0 - SMALL_AMOUNT, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        waveManager.addWave(bidirectional)
        waveManager.addWave(unidirectional)
        waveManager.setAmplitudeVariance(0.5)
        waveManager.setSpeedVariance(0.5)
        newLick()
    }

    fun newLick() {
        lickType = LickType.entries.random()
        lickStartPoint = Random.nextDouble()
        lickEndPoint = Random.nextDouble()

        val distance = abs(lickEndPoint - lickStartPoint)
        val maxSpeed = (-3.0 * distance) + 4.5
        val speed = Random.nextDouble(0.3, maxSpeed)
        val desiredTimeSeconds = Random.nextDouble(1.0, 5.0)
        val calculatedReps = (desiredTimeSeconds * speed).toInt().coerceAtLeast(1)

        waveManager.restart()
        waveManager.setSpeed(speed)
        waveManager.stopAfterIterations(calculatedReps) { newLick() }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        waveManager.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        val waveName = when (lickType) {
            LickType.UNIDIRECTIONAL -> "unidirectional"
            LickType.BIDIRECTIONAL -> "bidirectional"
        }
        val (position, velocity) = waveManager.getPositionAndVelocity(waveName)
        val lickPosition = (lickEndPoint - lickStartPoint) * position + lickStartPoint
        val scaledVelocity = scaleVelocity(velocity, 0.1)

        val freqA = lickPosition * 0.5 + 0.5
        val freqB = lickPosition * 0.52 + 0.48
        val (ampA, ampB) = calculatePositionalEffect(scaledVelocity, lickPosition, 1.0)

        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class PenetrationActivity : Activity() {
    override val displayName = "Penetration"
    override val iconResId = R.drawable.rocket
    var waveManager: WaveManager = WaveManager()
    val penetrationSpeedChangeSecsRange = 1.0..20.0
    val penetrationSpeedRange = 0.3..3.0
    val penetrationSpeedChangeRateRange = 0.05..0.3
    val penetrationFeelExponentRange = 1.0..2.0
    val penetrationFeelExponentChangeRateRange = 0.05..0.1
    val penetrationFeelChangeSecsRange = 2.0..10.0
    //var penetrationFeelExponent = 1.0

    private val penetrationFeelExponent = SmoothedValue(randomInRange(penetrationFeelExponentRange))

    init {
        val penetrationWave = CyclicalWave(
            WaveShape(
                name = "penetration",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(0.4, 0.95, 0.2),
                    WavePoint(0.5, 0.97, 0.0),
                    WavePoint(0.6, 0.95, -0.2),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        waveManager.addWave(penetrationWave)
        waveManager.setSpeedVariance(0.2)
        waveManager.setAmplitudeVariance(0.1)
        speedChange()
        feelChange()
        waveManager.setSpeed(0.15)
        waveManager.setTargetSpeed(0.5, 0.1)
    }

    fun speedChange() {
        val newSpeed = randomInRange(penetrationSpeedRange)
        val changeRate = randomInRange(penetrationSpeedChangeRateRange)
        val nextSpeedChangeSecs = randomInRange(penetrationSpeedChangeSecsRange)
        waveManager.setTargetSpeed(newSpeed, changeRate)
        timerManager.addTimer("speedChange", nextSpeedChangeSecs) {
            speedChange()
        }
    }

    fun feelChange() {
        val target = randomInRange(penetrationFeelExponentRange)
        val rate = randomInRange(penetrationFeelExponentChangeRateRange)
        penetrationFeelExponent.setTarget(target, rate)
        val nextFeelChangeSecs = randomInRange(penetrationFeelChangeSecsRange)
        timerManager.addTimer("feelChange", nextFeelChangeSecs) {
            feelChange()
        }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        penetrationFeelExponent.update(deltaSimulationTime)
        waveManager.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        val (position, velocity) = waveManager.getPositionAndVelocity("penetration")
        val scaledVelocity = scaleVelocity(velocity, 0.1)

        var ampFactor = (waveManager.currentSpeed - penetrationSpeedRange.start) / (penetrationSpeedRange.endInclusive - penetrationSpeedRange.start)
        ampFactor = 0.8 + ampFactor * 0.2

        val freqBaseA = position * 0.7
        val freqBaseB = scaledVelocity * 0.5 + position * 0.4
        val freqA = freqBaseA.pow(penetrationFeelExponent.current).coerceIn(0.0,1.0)
        val freqB = freqBaseB.pow(penetrationFeelExponent.current).coerceIn(0.0,1.0)

        val ampA = position * ampFactor
        val ampB = (scaledVelocity * 0.6 + position * 0.4) * ampFactor

        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class VibroActivity : Activity() {
    override val displayName = "Sliding vibrator"
    override val iconResId = R.drawable.vibration
    val vibeSpeedRange = 0.0..1.0
    val vibeMoveSpeedRange = 0.04..0.3
    val vibeHoldTimeRange = 0.0..3.0
    val vibeSpeedChangeTimeRange = 5.0..30.0
    val vibePower = 0.9
    val vibePositionRange = 0.0 .. 1.0
    val vibeHoldProbability = 0.5

    var vibeSpeed = 0.3
    var vibeTargetPosition = 0.5
    var vibeMoveSpeed = 0.1
    private val vibePosition = SmoothedValue(randomInRange(vibePositionRange))

    init {
        initializeWithRandomValues()
    }

    private fun initializeWithRandomValues() {
        vibeSpeed = randomInRange(vibeSpeedRange)
        vibePosition.setImmediately(randomInRange(vibePositionRange))
        newTarget()
        scheduleSpeedChangeTimer()
    }

    private fun newTarget() {
        vibeTargetPosition = randomInRange(vibePositionRange)
        vibeMoveSpeed = randomInRange(vibeMoveSpeedRange)
        vibePosition.setTarget(vibeTargetPosition, vibeMoveSpeed, onReached = { targetPositionReached() } )
    }

    private fun scheduleSpeedChangeTimer() {
        timerManager.addTimer("speedChange", randomInRange(vibeSpeedChangeTimeRange)) {
            vibeSpeed = randomInRange(vibeSpeedRange)
            scheduleSpeedChangeTimer() // Reschedule
        }
    }

    private fun scheduleHoldTimer() {
        timerManager.addTimer("hold", randomInRange(vibeHoldTimeRange)) {
            newTarget()
        }
    }

    private fun targetPositionReached() {
        if (Random.nextDouble() < vibeHoldProbability) {
            scheduleHoldTimer()
        }
        else {
            newTarget()
        }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        vibePosition.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        val freqA = vibeSpeed
        val freqB = vibeSpeed
        val (ampA, ampB) = calculatePositionalEffect(vibePower, vibePosition.current, 1.0)

        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class MilkerActivity : Activity() {
    override val displayName = "Milkmaster 3000"
    override val iconResId = R.drawable.cow
    val waveManager: WaveManager = WaveManager()
    val wompStartFreq = 0.0
    val wompEndFreq = 0.7
    /*val wompStartSpeed = 0.2
    val wompEndSpeed = 3.0
    val wompRateChangeSpeedRange = 0.15..0.2*/
    val wompStartSpeed = 0.3
    val wompEndSpeed = 2.5
    val wompRateChangeSpeedRange = 0.1..0.15
    val buzzFreqARange = 0.0..0.3
    val buzzFreqBRange = 0.7..1.0
    val buzzSpeedRange = 0.4..0.8
    val buzzDurationRange = 6.0..12.0

    var currentStage = MilkerStage.Womp
    var buzzFreqAStart = 0.75
    var buzzFreqAEnd = 0.75
    var buzzFreqBStart = 0.75
    var buzzFreqBEnd = 0.75
    var reverseWomp = false

    enum class MilkerStage {
        Womp,
        Buzz,
    }

    init {
        val wompWave = CyclicalWave(
            WaveShape(
                name = "womp",
                points = listOf(
                    WavePoint(0.0, 1.0, 0.0),
                    WavePoint(1.0 - SMALL_AMOUNT, 0.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        val buzzWave = CyclicalWave(
            WaveShape(
                name = "buzz",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(0.5, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )

        waveManager.addWave(wompWave)
        waveManager.addWave(buzzWave)
        waveManager.setSpeedVariance(0.0)
        waveManager.setAmplitudeVariance(0.0)
        wompStart()
    }

    fun wompStart() {
        currentStage = MilkerStage.Womp
        waveManager.restart()
        val speedChangeRate = randomInRange(wompRateChangeSpeedRange)
        reverseWomp = Random.nextBoolean()
        waveManager.setSpeed(wompStartSpeed)
        waveManager.setTargetSpeed(wompEndSpeed, speedChangeRate) {
            waveManager.stopAtEndOfCycle { buzzStart() }
        }
    }

    fun buzzStart() {
        currentStage = MilkerStage.Buzz
        buzzFreqAStart = randomInRange(buzzFreqARange)
        buzzFreqAEnd = randomInRange(buzzFreqARange)
        buzzFreqBStart = randomInRange(buzzFreqBRange)
        buzzFreqBEnd = randomInRange(buzzFreqBRange)
        val speed = randomInRange(buzzSpeedRange)
        waveManager.restart()
        waveManager.setSpeed(speed)
        timerManager.addTimer("buzzEnd", randomInRange(buzzDurationRange)) {
            wompStart()
        }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        waveManager.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        var ampA = 0.0
        var ampB = 0.0
        var freqA = 0.0
        var freqB = 0.0

        when (currentStage) {
            MilkerStage.Womp -> {
                val position = waveManager.getPosition("womp")
                var adjustedPosition = if (reverseWomp) 1 - position else position
                val (posAmpA, posAmpB) = calculatePositionalEffect(0.9, adjustedPosition, 1.0)
                ampA = posAmpA
                ampB = posAmpB
                freqA = (adjustedPosition * (wompEndFreq - wompStartFreq)) + wompStartFreq
                freqB = (adjustedPosition * (wompEndFreq - wompStartFreq)) + wompStartFreq
            }
            MilkerStage.Buzz -> {
                val position = waveManager.getPosition("buzz")
                val phase = timerManager.getProportionElapsed("buzzEnd") ?: 1.0
                val freqRangeA = buzzFreqAEnd - buzzFreqAStart
                val freqRangeB = buzzFreqBEnd - buzzFreqBStart

                freqA = phase * freqRangeA + buzzFreqAStart
                freqB = phase * freqRangeB + buzzFreqBStart
                ampA = 0.8 + 0.1 * position
                ampB = 0.8 + 0.1 * position
            }
        }

        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class ChaosActivity : Activity() {
    override val displayName = "Chaos"
    override val iconResId = R.drawable.chaos
    val freqRange = 0.0..1.0
    val ampRange = 0.0..1.0
    val randomiseEveryCyclesChangeSecsRange = 10.0..30.0
    val randomiseEveryCyclesRange = 1..10
    var randomiseEveryCycles = 1
    var cycleCounter = 0
    var ampA = 0.0
    var ampB = 0.0
    var freqA = 0.0
    var freqB = 0.0

    init {
        randomise()
        randomiseEveryCyclesChange()
    }

    fun randomise() {
        ampA = randomInRange(ampRange)
        ampB = randomInRange(ampRange)
        freqA = randomInRange(freqRange)
        freqB = randomInRange(freqRange)
    }

    fun randomiseEveryCyclesChange() {
        randomiseEveryCycles = randomInRange(randomiseEveryCyclesRange)
        val nextChangeSecs = randomInRange(randomiseEveryCyclesChangeSecsRange)
        timerManager.addTimer("randomiseEveryCyclesChange", nextChangeSecs) {
            randomiseEveryCyclesChange()
        }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        cycleCounter += 1
        if (cycleCounter >= randomiseEveryCycles) {
            cycleCounter = 0
            randomise()
        }
    }

    override fun getPulse(): Pulse {
        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class LuxuryHJActivity : Activity() {
    override val displayName = "Luxury HJ"
    override val iconResId = R.drawable.hand
    val hjWaveManager: WaveManager = WaveManager()
    val bonusWaveManager: WaveManager = WaveManager()
    val hjSpeedChangeSecsRange = 1.0..20.0
    val hjSpeedRange = 0.3..3.0
    val hjSpeedChangeRateRange = 0.05..0.3
    val bonusTimeRange = 10.0..25.0
    val bonusSpeedRange = 1.5..5.0
    val startFreq = 0.15
    val endFreq = 0.65
    val bonusStartFreq = 0.8
    val bonusEndFreq = 1.0

    init {
        val hjWave = CyclicalWave(
            WaveShape(
                name = "hj",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(0.5, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        val bonusWave = CyclicalWave(
            WaveShape(
                name = "bonus",
                points = listOf(
                    WavePoint(0.0, 0.6, 0.0),
                    WavePoint(0.5, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        hjWaveManager.addWave(hjWave)
        bonusWaveManager.addWave(bonusWave)

        hjWaveManager.setSpeedVariance(0.2)
        hjWaveManager.setAmplitudeVariance(0.2)
        hjWaveManager.setAmplitudeVarianceEaseIn(1.0)
        bonusWaveManager.setSpeedVariance(0.5)
        bonusWaveManager.setAmplitudeVariance(0.15)
        bonusWaveManager.setSpeed(1.0)
        hjWaveManager.setSpeed(0.5)
        speedChange()
    }

    fun speedChange() {
        val newSpeed = randomInRange(hjSpeedRange)
        val changeRate = randomInRange(hjSpeedChangeRateRange)
        val nextSpeedChangeSecs = randomInRange(hjSpeedChangeSecsRange)
        hjWaveManager.setTargetSpeed(newSpeed, changeRate)
        timerManager.addTimer("speedChange", nextSpeedChangeSecs) {
            speedChange()
        }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        hjWaveManager.update(deltaSimulationTime)
        bonusWaveManager.update(deltaSimulationTime)
        val bonusAProbability = (0.7 * deltaSimulationTime) / 60.0
        val bonusBProbability = (0.7 * deltaSimulationTime) / 60.0
        if (Random.nextDouble() < bonusAProbability && timerManager.hasTimer("bonusB") == false) {
            bonusWaveManager.setSpeed(randomInRange(bonusSpeedRange))
            timerManager.addTimer("bonusA", randomInRange(bonusTimeRange)) {}
        }
        if (Random.nextDouble() < bonusBProbability && timerManager.hasTimer("bonusA") == false) {
            bonusWaveManager.setSpeed(randomInRange(bonusSpeedRange))
            timerManager.addTimer("bonusB", randomInRange(bonusTimeRange)) {}
        }
    }

    override fun getPulse(): Pulse {
        var freqA = 0.0
        var freqB = 0.0

        val position = hjWaveManager.getPosition("hj", applyAmplitudeVariance = false)
        val baseAmp = hjWaveManager.currentAmplitude
        var (ampA, ampB) = calculatePositionalEffect(baseAmp, position, 1.0)
        freqA = ((position * (endFreq - startFreq)) + startFreq) * 0.98
        freqB = (position * (endFreq - startFreq)) + startFreq

        val ampBonus = bonusWaveManager.getPosition("bonus")
        val freqBonus = (ampBonus * (bonusEndFreq - bonusStartFreq)) + bonusStartFreq

        val bonusWeight = 0.7

        if(timerManager.hasTimer("bonusA")) {
            ampA = ampBonus * bonusWeight + (ampA * (1.0 - bonusWeight))
            freqA = freqBonus * bonusWeight + (freqA * (1.0 - bonusWeight))
        }
        if(timerManager.hasTimer("bonusB")) {
            ampB = ampBonus * bonusWeight + (ampB * (1.0 - bonusWeight))
            freqB = freqBonus * bonusWeight + (freqB * (1.0 - bonusWeight))
        }

        return Pulse(
            freqA = freqA.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class OppositesActivity : Activity() {
    override val displayName = "Opposites"
    override val iconResId = R.drawable.yin_yang

    val ampRange = 0.0..1.0
    val freqRange = 0.0..1.0
    val overallSpeedRange = 0.5..3.0
    val overallSpeedChangeRateRange = 0.2..0.5
    val overallSpeedChangeSecsRange = 10.0..20.0
    val baseChangeRateRange = 0.15..0.4

    private val ampA = SmoothedValue(randomInRange(ampRange))
    private val freqA = SmoothedValue(randomInRange(freqRange))
    private val overallSpeed = SmoothedValue(randomInRange(overallSpeedRange))

    init {
        newRandomAmplitudeTarget(ampA)
        newRandomFrequencyTarget(freqA)
        speedChange()
    }

    private fun getChangeRate() : Double {
        val range = baseChangeRateRange.start * overallSpeed.current .. baseChangeRateRange.endInclusive * overallSpeed.current
        return randomInRange(range)
    }

    private fun speedChange() {
        val newSpeed = randomInRange(overallSpeedRange)
        val changeRate = randomInRange(overallSpeedChangeRateRange)
        val nextSpeedChangeSecs = randomInRange(overallSpeedChangeSecsRange)
        overallSpeed.setTarget(newSpeed, changeRate)
        timerManager.addTimer("speedChange", nextSpeedChangeSecs) {
            speedChange()
        }
    }

    private fun newRandomFrequencyTarget(freq: SmoothedValue) {
        val targetFreq = randomInRange(freqRange)
        val changeRate = getChangeRate()
        freq.setTarget(
            target = targetFreq,
            rate = changeRate,
            onReached = { newRandomFrequencyTarget(freq) }
        )
    }

    private fun newRandomAmplitudeTarget(amp: SmoothedValue) {
        val targetAmp = randomInRange(ampRange)
        val changeRate = getChangeRate()
        amp.setTarget(
            target = targetAmp,
            rate = changeRate,
            onReached = { newRandomAmplitudeTarget(amp) }
        )
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)

        ampA.update(deltaSimulationTime)
        freqA.update(deltaSimulationTime)
        overallSpeed.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        val ampB = 1.0 - ampA.current
        val freqB = 1.0 - freqA.current
        return Pulse(
            freqA = freqA.current.toFloat(),
            freqB = freqB.toFloat(),
            ampA = ampA.current.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class Calibration1Activity : Activity() {
    override val displayName = "Calibration 1"
    override val iconResId = R.drawable.swapvert
    val calibrationWaveManager: WaveManager = WaveManager()
    val waveSpeed = 0.25
    val waveFrequency = 0.5
    val wavePower = 0.9

    init {
        val calibrationWave = CyclicalWave(
            WaveShape(
                name = "calibration",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(0.5, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        calibrationWaveManager.addWave(calibrationWave)
        calibrationWaveManager.setSpeed(waveSpeed)
        calibrationWaveManager.setSpeedVariance(0.0)
        calibrationWaveManager.setAmplitudeVariance(0.0)
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        calibrationWaveManager.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        val position = calibrationWaveManager.getPosition("calibration")
        var (ampA, ampB) = calculatePositionalEffect(wavePower, position, 1.0)

        return Pulse(
            freqA = waveFrequency.toFloat(),
            freqB = waveFrequency.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}

class Calibration2Activity : Activity() {
    override val displayName = "Calibration 2"
    override val iconResId = R.drawable.calibration
    val calibrationWaveManager: WaveManager = WaveManager()
    val waveSpeed = 0.25
    val wavePower = 0.9
    val phaseChangeSecs = 16.0
    var currentPhase: Phase = Phase.CHANNEL_A

    enum class Phase {
        CHANNEL_A,
        CHANNEL_B,
        BOTH,
    }

    init {
        val calibrationWave = CyclicalWave(
            WaveShape(
                name = "calibration",
                points = listOf(
                    WavePoint(0.0, 0.0, 0.0),
                    WavePoint(0.5, 1.0, 0.0),
                ),
                interpolationType = InterpolationType.HERMITE
            )
        )
        calibrationWaveManager.addWave(calibrationWave)
        calibrationWaveManager.setSpeed(waveSpeed)
        calibrationWaveManager.setSpeedVariance(0.0)
        calibrationWaveManager.setAmplitudeVariance(0.0)
        timerManager.addTimer("nextPhase", phaseChangeSecs) {
            nextPhase()
        }
    }

    fun nextPhase() {
        currentPhase = currentPhase.next()
        timerManager.addTimer("nextPhase", phaseChangeSecs) {
            nextPhase()
        }
    }

    override fun runSimulation(deltaSimulationTime: Double) {
        super.runSimulation(deltaSimulationTime)
        calibrationWaveManager.update(deltaSimulationTime)
    }

    override fun getPulse(): Pulse {
        val frequency = calibrationWaveManager.getPosition("calibration")
        var ampA = if (currentPhase == Phase.CHANNEL_B) 0.0 else wavePower
        var ampB = if (currentPhase == Phase.CHANNEL_A) 0.0 else wavePower

        return Pulse(
            freqA = frequency.toFloat(),
            freqB = frequency.toFloat(),
            ampA = ampA.toFloat(),
            ampB = ampB.toFloat()
        )
    }
}