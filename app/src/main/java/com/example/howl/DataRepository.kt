package com.example.howl

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.Boolean
import kotlin.time.TimeMark
import kotlin.Float

const val showDeveloperOptions = false

object DataRepository {
    var database: HowlDatabase? = null

    private val _pulseHistory: MutableStateFlow<List<Pulse>> = MutableStateFlow(emptyList())
    val pulseHistory: StateFlow<List<Pulse>> = _pulseHistory.asStateFlow()
    const val MAX_HISTORY_SIZE = 200

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

    fun addPulsesToHistory(newPulses: List<Pulse>) {
        _pulseHistory.update {
            _pulseHistory.value.toMutableList().apply {
                this.addAll(newPulses)
                while (this.size > MAX_HISTORY_SIZE)
                    this.removeAt(0)
            }
        }
    }

    fun clearPulseHistory() {
        _pulseHistory.update { emptyList() }
    }

    fun initialise(db: HowlDatabase) {
        database = db
    }

    suspend fun saveSettings() {
        Log.v("Howl", "Saving settings")
        val settings = SavedSettings(
            channelALimit = coyoteParametersState.value.channelALimit,
            channelBLimit = coyoteParametersState.value.channelBLimit,
            channelAIntensityBalance = coyoteParametersState.value.channelAIntensityBalance,
            channelBIntensityBalance = coyoteParametersState.value.channelBIntensityBalance,
            channelAFrequencyBalance = coyoteParametersState.value.channelAFrequencyBalance,
            channelBFrequencyBalance = coyoteParametersState.value.channelBFrequencyBalance,
            frequencyInversionA = playerAdvancedControlsState.value.frequencyInversionA,
            frequencyInversionB = playerAdvancedControlsState.value.frequencyInversionB,
            frequencyModEnable = playerAdvancedControlsState.value.frequencyModEnable,
            frequencyModStrength = playerAdvancedControlsState.value.frequencyModStrength,
            frequencyModPeriod = playerAdvancedControlsState.value.frequencyModPeriod,
            frequencyModInvert = playerAdvancedControlsState.value.frequencyModInvert,
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
            powerStepSizeA = miscOptionsState.value.powerStepSizeA,
            powerStepSizeB = miscOptionsState.value.powerStepSizeB
        )
        database?.savedSettingsDao()?.updateSettings(settings)
    }

    suspend fun loadSettings(){
        Log.v("Howl", "Loading settings")
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
            frequencyInversionA = settings.frequencyInversionA,
            frequencyInversionB = settings.frequencyInversionB,
            frequencyModEnable = settings.frequencyModEnable,
            frequencyModStrength = settings.frequencyModStrength,
            frequencyModPeriod = settings.frequencyModPeriod,
            frequencyModInvert = settings.frequencyModInvert,
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
            powerStepSizeA = settings.powerStepSizeA,
            powerStepSizeB = settings.powerStepSizeB
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
        val frequencyInversionA: Boolean = false,
        val frequencyInversionB: Boolean = false,
        val frequencyModEnable: Boolean = false,
        val frequencyModStrength: Float = 0.1f,
        val frequencyModPeriod: Float = 1.0f,
        val frequencyModInvert: Boolean = false,
        var funscriptVolume: Float = 0.5f,
        val funscriptPositionalEffectStrength: Float = 1.0f,
        var funscriptFeel: Float = 1.0f,
        var funscriptFrequencyTimeOffset: Float = 0.1f,
    )

    data class MainOptionsState (
        val channelAPower: Int = 0,
        val channelBPower: Int = 0,
        val globalMute: Boolean = false,
        val swapChannels: Boolean = false,
        val frequencyRange: ClosedFloatingPointRange<Float> = 10f..100f,
        val pulseChartMode: PulseChartMode = PulseChartMode.Off
    )

    data class MiscOptionsState (
        val powerStepSizeA: Int = 1,
        val powerStepSizeB: Int = 1,
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