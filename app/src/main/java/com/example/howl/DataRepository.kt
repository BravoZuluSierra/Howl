package com.example.howl

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Boolean
import kotlin.time.TimeMark
import kotlin.Float

const val showDeveloperOptions = false
const val howlVersion = "0.4"

object DataRepository {
    var database: HowlDatabase? = null

    private val _lastPulse = MutableStateFlow(Pulse())
    val lastPulse: StateFlow<Pulse> = _lastPulse.asStateFlow()

    private val _pulseHistoryVersion = MutableStateFlow(0)
    val pulseHistoryVersion: StateFlow<Int> = _pulseHistoryVersion.asStateFlow()

    const val PULSE_HISTORY_SIZE = 200
    val pulseHistoryDeque: ArrayDeque<Pulse> = ArrayDeque(PULSE_HISTORY_SIZE)
    private val pulseHistoryMutex = Mutex()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playerAdvancedControlsState = MutableStateFlow(PlayerAdvancedControlsState())
    val playerAdvancedControlsState: StateFlow<PlayerAdvancedControlsState> = _playerAdvancedControlsState.asStateFlow()

    private val _mainOptionsState = MutableStateFlow(MainOptionsState())
    val mainOptionsState: StateFlow<MainOptionsState> = _mainOptionsState.asStateFlow()

    private val _miscOptionsState = MutableStateFlow(MiscOptionsState())
    val miscOptionsState: StateFlow<MiscOptionsState> = _miscOptionsState.asStateFlow()

    private val _developerOptionsState = MutableStateFlow(DeveloperOptionsState())
    val developerOptionsState: StateFlow<DeveloperOptionsState> = _developerOptionsState.asStateFlow()

    private val _generatorState = MutableStateFlow(GeneratorState())
    val generatorState: StateFlow<GeneratorState> = _generatorState.asStateFlow()

    private val _activityState = MutableStateFlow(ActivityState())
    val activityState: StateFlow<ActivityState> = _activityState.asStateFlow()

    private val _coyoteBatteryLevel = MutableStateFlow(0)
    val coyoteBatteryLevel: StateFlow<Int> = _coyoteBatteryLevel.asStateFlow()

    private val _coyoteConnectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val coyoteConnectionStatus: StateFlow<ConnectionStatus> = _coyoteConnectionStatus.asStateFlow()

    private val _coyoteParametersState = MutableStateFlow(DGCoyote.Parameters())
    val coyoteParametersState: StateFlow<DGCoyote.Parameters> = _coyoteParametersState.asStateFlow()

    fun setCoyoteParametersState(newCoyoteParametersState: DGCoyote.Parameters) {
        _coyoteParametersState.update { newCoyoteParametersState }
    }

    fun setActivityState(newActivityState: ActivityState) {
        _activityState.update { newActivityState }
    }

    fun setPlayerState(newPlayerState: PlayerState) {
        _playerState.update { newPlayerState }
    }

    fun setPlayerAdvancedControlsState(newPlayerAdvancedControlsState: PlayerAdvancedControlsState) {
        _playerAdvancedControlsState.update { newPlayerAdvancedControlsState }
    }

    fun setMainOptionsState(newMainOptionsState: MainOptionsState) {
        _mainOptionsState.update { newMainOptionsState }
    }

    fun setMiscOptionsState(newMiscOptionsState: MiscOptionsState) {
        _miscOptionsState.update { newMiscOptionsState }
    }

    fun setDeveloperOptionsState(newDeveloperOptionsState: DeveloperOptionsState) {
        _developerOptionsState.update { newDeveloperOptionsState }
    }

    fun setCoyoteBatteryLevel(percent: Int) {
        _coyoteBatteryLevel.update { percent }
    }

    fun setCoyoteConnectionStatus(status: ConnectionStatus) {
        _coyoteConnectionStatus.update { status }
    }

    fun setChannelAPower(power: Int) {
        val newPower: Int = power.coerceIn(0..coyoteParametersState.value.channelALimit)
        _mainOptionsState.update { it.copy(channelAPower = newPower)}
    }

    fun setChannelBPower(power: Int) {
        val newPower: Int = power.coerceIn(0..coyoteParametersState.value.channelBLimit)
        _mainOptionsState.update { it.copy(channelBPower = newPower)}
    }

    fun setGlobalMute(muted: Boolean) {
        _mainOptionsState.update { it.copy(globalMute = muted)}
    }

    fun setAutoIncreasePower(autoIncrease: Boolean) {
        _mainOptionsState.update { it.copy(autoIncreasePower = autoIncrease)}
    }

    fun setPulseChartMode(mode: PulseChartMode) {
        _mainOptionsState.update { it.copy(pulseChartMode = mode)}
    }

    fun setSwapChannels(swap: Boolean) {
        _mainOptionsState.update { it.copy(swapChannels = swap)}
    }

    fun setFrequencyRange(range: ClosedFloatingPointRange<Float>) {
        _mainOptionsState.update { it.copy(frequencyRange = range) }
    }

    fun setPlayerPosition(position: Double) {
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun setPlayerPulseSource(source: PulseSource?) {
        _playerState.update { it.copy(activePulseSource = source) }
    }

    fun setGeneratorState(newGeneratorState: GeneratorState) {
        _generatorState.update { newGeneratorState }
    }

    suspend fun addPulsesToHistory(newPulses: List<Pulse>) {
        pulseHistoryMutex.withLock {
            pulseHistoryDeque.addAll(newPulses)
            while (pulseHistoryDeque.size > PULSE_HISTORY_SIZE) {
                pulseHistoryDeque.removeFirst()
            }
            _lastPulse.update { pulseHistoryDeque.last() }
            _pulseHistoryVersion.update { it + 1 }
        }
    }

    suspend fun clearPulseHistory() {
        pulseHistoryMutex.withLock {
            pulseHistoryDeque.clear()
            _lastPulse.value = Pulse()
            _pulseHistoryVersion.update { it + 1 }
        }
    }

    suspend fun getPulseHistory(): List<Pulse> {
        return pulseHistoryMutex.withLock {
            pulseHistoryDeque.toList()
        }
    }

    fun initialise(db: HowlDatabase) {
        database = db
    }

    suspend fun saveSettings() {
        HLog.d("DataRepository", "Saving settings")
        val settings = SavedSettings(
            channelALimit = coyoteParametersState.value.channelALimit,
            channelBLimit = coyoteParametersState.value.channelBLimit,
            channelAIntensityBalance = coyoteParametersState.value.channelAIntensityBalance,
            channelBIntensityBalance = coyoteParametersState.value.channelBIntensityBalance,
            channelAFrequencyBalance = coyoteParametersState.value.channelAFrequencyBalance,
            channelBFrequencyBalance = coyoteParametersState.value.channelBFrequencyBalance,
            playbackSpeed = playerAdvancedControlsState.value.playbackSpeed,
            frequencyInversionA = playerAdvancedControlsState.value.frequencyInversionA,
            frequencyInversionB = playerAdvancedControlsState.value.frequencyInversionB,
            funscriptVolume = playerAdvancedControlsState.value.funscriptVolume,
            funscriptPositionalEffectStrength = playerAdvancedControlsState.value.funscriptPositionalEffectStrength,
            funscriptFeel = playerAdvancedControlsState.value.funscriptFeel,
            funscriptFrequencyTimeOffset = playerAdvancedControlsState.value.funscriptFrequencyTimeOffset,
            autoChange = generatorState.value.autoChange,
            speedChangeProbability = generatorState.value.speedChangeProbability,
            amplitudeChangeProbability = generatorState.value.amplitudeChangeProbability,
            frequencyChangeProbability = generatorState.value.frequencyChangeProbability,
            waveChangeProbability = generatorState.value.waveChangeProbability,
            activityChangeProbability = activityState.value.activityChangeProbability,
            showPowerMeter = miscOptionsState.value.showPowerMeter,
            smootherCharts = miscOptionsState.value.smootherCharts,
            showDebugLog = miscOptionsState.value.showDebugLog,
            powerStepSizeA = miscOptionsState.value.powerStepSizeA,
            powerStepSizeB = miscOptionsState.value.powerStepSizeB,
            powerAutoIncrementDelayA = miscOptionsState.value.powerAutoIncrementDelayA,
            powerAutoIncrementDelayB = miscOptionsState.value.powerAutoIncrementDelayB
        )
        database?.savedSettingsDao()?.updateSettings(settings)
    }

    suspend fun loadSettings(){
        HLog.d("DataRepository", "Loading settings")
        var settings = database?.savedSettingsDao()?.getSettings()
        if (settings==null)
            settings = SavedSettings()
        setCoyoteParametersState(DGCoyote.Parameters(
            channelALimit = settings.channelALimit,
            channelBLimit = settings.channelBLimit,
            channelAIntensityBalance = settings.channelAIntensityBalance,
            channelBIntensityBalance = settings.channelBIntensityBalance,
            channelAFrequencyBalance = settings.channelAFrequencyBalance,
            channelBFrequencyBalance = settings.channelBFrequencyBalance
        ))
        setPlayerAdvancedControlsState(PlayerAdvancedControlsState(
            playbackSpeed = settings.playbackSpeed,
            frequencyInversionA = settings.frequencyInversionA,
            frequencyInversionB = settings.frequencyInversionB,
            funscriptVolume = settings.funscriptVolume,
            funscriptPositionalEffectStrength = settings.funscriptPositionalEffectStrength,
            funscriptFeel = settings.funscriptFeel,
            funscriptFrequencyTimeOffset = settings.funscriptFrequencyTimeOffset
        ))
        setGeneratorState(generatorState.value.copy(
            autoChange = settings.autoChange,
            speedChangeProbability = settings.speedChangeProbability,
            amplitudeChangeProbability = settings.amplitudeChangeProbability,
            frequencyChangeProbability = settings.frequencyChangeProbability,
            waveChangeProbability = settings.waveChangeProbability,
        ))
        setMiscOptionsState(MiscOptionsState(
            showPowerMeter = settings.showPowerMeter,
            smootherCharts = settings.smootherCharts,
            showDebugLog = settings.showDebugLog,
            powerStepSizeA = settings.powerStepSizeA,
            powerStepSizeB = settings.powerStepSizeB,
            powerAutoIncrementDelayA = settings.powerAutoIncrementDelayA,
            powerAutoIncrementDelayB = settings.powerAutoIncrementDelayB
        ))
        setActivityState(activityState.value.copy(
            activityChangeProbability = settings.activityChangeProbability
        ))
    }

    data class ActivityState(
        val currentActivityDisplayName: String = "",
        val activityChangeProbability: Float = 0.0f,
    )

    data class GeneratorState(
        val initialised: Boolean = false,
        val channelAInfo: GeneratorChannelInfo = GeneratorChannelInfo(),
        val channelBInfo: GeneratorChannelInfo = GeneratorChannelInfo(),
        val autoChange: Boolean = true,
        val speedChangeProbability: Double = 0.2,
        val amplitudeChangeProbability: Double = 0.2,
        val frequencyChangeProbability: Double = 0.2,
        val waveChangeProbability: Double = 0.2,
    )

    data class PlayerState(
        val activePulseSource: PulseSource? = null,
        val currentPosition: Double = 0.0,
        val startPosition: Double = 0.0,
        val isPlaying: Boolean = false,
        val startTime: TimeMark? = null
    )

    data class PlayerAdvancedControlsState (
        val playbackSpeed: Float = 1.0f,
        val frequencyInversionA: Boolean = false,
        val frequencyInversionB: Boolean = false,
        var funscriptVolume: Float = 0.5f,
        val funscriptPositionalEffectStrength: Float = 1.0f,
        var funscriptFeel: Float = 1.0f,
        var funscriptFrequencyTimeOffset: Float = 0.1f,
    )

    data class MainOptionsState (
        val channelAPower: Int = 0,
        val channelBPower: Int = 0,
        val globalMute: Boolean = false,
        val autoIncreasePower: Boolean = false,
        val swapChannels: Boolean = false,
        val frequencyRange: ClosedFloatingPointRange<Float> = 10f..100f,
        val pulseChartMode: PulseChartMode = PulseChartMode.Off
    )

    data class MiscOptionsState (
        val showPowerMeter: Boolean = true,
        val smootherCharts: Boolean = true,
        val showDebugLog: Boolean = false,
        val powerStepSizeA: Int = 1,
        val powerStepSizeB: Int = 1,
        val powerAutoIncrementDelayA: Int = 120,
        val powerAutoIncrementDelayB: Int = 120,
    )

    data class DeveloperOptionsState (
        val developerFrequencyExponent: Float = 1.0f,
        val developerFrequencyGain: Float = 1.0f,
        val developerFrequencyAdjustA: Float = 0.0f,
        val developerFrequencyAdjustB: Float = 0.0f,
        val developerAmplitudeExponent: Float = 1.0f,
        val developerAmplitudeGain: Float = 1.0f,
    )
}