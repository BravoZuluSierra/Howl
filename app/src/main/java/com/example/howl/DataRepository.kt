package com.example.howl

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.Boolean
import kotlin.time.TimeMark

object DataRepository {
    var database: HowlDatabase? = null

    private val _funscriptPlayerState = MutableStateFlow(FunscriptPlayerState())
    val funscriptPlayerState: StateFlow<FunscriptPlayerState> = _funscriptPlayerState.asStateFlow()

    private val _funscriptAdvancedControlsState = MutableStateFlow(FunscriptAdvancedControlsState())
    val funscriptAdvancedControlsState: StateFlow<FunscriptAdvancedControlsState> = _funscriptAdvancedControlsState.asStateFlow()

    private val _mainOptionsState = MutableStateFlow(MainOptionsState())
    val mainOptionsState: StateFlow<MainOptionsState> = _mainOptionsState.asStateFlow()

    private val _miscOptionsState = MutableStateFlow(MiscOptionsState())
    val miscOptionsState: StateFlow<MiscOptionsState> = _miscOptionsState.asStateFlow()

    private val _generatorState = MutableStateFlow(GeneratorState())
    val generatorState: StateFlow<GeneratorState> = _generatorState.asStateFlow()

    private val _coyoteBatteryLevel = MutableStateFlow(0)
    val coyoteBatteryLevel: StateFlow<Int> = _coyoteBatteryLevel.asStateFlow()

    private val _coyoteConnectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val coyoteConnectionStatus: StateFlow<ConnectionStatus> = _coyoteConnectionStatus.asStateFlow()

    private val _coyoteParametersState = MutableStateFlow(DGCoyote.Parameters())
    val coyoteParametersState: StateFlow<DGCoyote.Parameters> = _coyoteParametersState.asStateFlow()

    fun setCoyoteParametersState(newCoyoteParametersState: DGCoyote.Parameters) {
        _coyoteParametersState.update { newCoyoteParametersState }
    }

    fun setFunscriptPlayerState(newFunscriptPlayerState: FunscriptPlayerState) {
        _funscriptPlayerState.update { newFunscriptPlayerState }
    }

    fun setFunscriptAdvancedControlsState(newFunscriptAdvancedControlsState: FunscriptAdvancedControlsState) {
        _funscriptAdvancedControlsState.update { newFunscriptAdvancedControlsState }
    }

    fun setMainOptionsState(newMainOptionsState: MainOptionsState) {
        _mainOptionsState.update { newMainOptionsState }
    }

    fun setMiscOptionsState(newMiscOptionsState: MiscOptionsState) {
        _miscOptionsState.update { newMiscOptionsState }
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

    fun setSwapChannels(swap: Boolean) {
        _mainOptionsState.update { it.copy(swapChannels = swap)}
    }

    fun setFrequencyRange(range: ClosedFloatingPointRange<Float>) {
        _mainOptionsState.update { it.copy(frequencyRange = range) }
    }

    fun setFunscriptPlayerPosition(position: Double) {
        _funscriptPlayerState.update { it.copy(currentPosition = position) }
    }

    fun setGeneratorState(newGeneratorState: GeneratorState) {
        _generatorState.update { newGeneratorState }
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
            frequencyInversionA = funscriptAdvancedControlsState.value.frequencyInversionA,
            frequencyInversionB = funscriptAdvancedControlsState.value.frequencyInversionB,
            channelBiasFactor = funscriptAdvancedControlsState.value.channelBiasFactor,
            frequencyModEnable = funscriptAdvancedControlsState.value.frequencyModEnable,
            frequencyModStrength = funscriptAdvancedControlsState.value.frequencyModStrength,
            frequencyModPeriod = funscriptAdvancedControlsState.value.frequencyModPeriod,
            frequencyModInvert = funscriptAdvancedControlsState.value.frequencyModInvert,
            autoCycle = generatorState.value.autoCycle,
            autoCycleTime = generatorState.value.autoCycleTime,
            powerStepSize = miscOptionsState.value.powerStepSize
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
        setFunscriptAdvancedControlsState(FunscriptAdvancedControlsState(
            frequencyInversionA = settings.frequencyInversionA,
            frequencyInversionB = settings.frequencyInversionB,
            channelBiasFactor = settings.channelBiasFactor,
            frequencyModEnable = settings.frequencyModEnable,
            frequencyModStrength = settings.frequencyModStrength,
            frequencyModPeriod = settings.frequencyModPeriod,
            frequencyModInvert = settings.frequencyModInvert
        ))
        setGeneratorState(generatorState.value.copy(
            autoCycle = settings.autoCycle,
            autoCycleTime = settings.autoCycleTime
        ))
        setMiscOptionsState(MiscOptionsState(
            powerStepSize = settings.powerStepSize
        ))
    }

    data class GeneratorState(
        val initialised: Boolean = false,
        val isPlaying: Boolean = false,
        val patternStartTime: TimeMark? = null,
        val channelAParameters: GeneratorParameters = GeneratorParameters(),
        val channelBParameters: GeneratorParameters = GeneratorParameters(),
        val autoCycle: Boolean = false,
        val autoCycleTime: Int = 90
    )

    data class FunscriptPlayerState(
        val filename: String? = null,
        val currentPosition: Double = 0.0,
        val startPosition: Double = 0.0,
        val isPlaying: Boolean = false,
        val fileLength: Double = 0.0,
        val startTime: TimeMark? = null
    )

    data class FunscriptAdvancedControlsState (
        val frequencyInversionA: Boolean = false,
        val frequencyInversionB: Boolean = false,
        val channelBiasFactor: Float = 0.7f,
        val frequencyModEnable: Boolean = false,
        val frequencyModStrength: Float = 0.1f,
        val frequencyModPeriod: Float = 1.0f,
        val frequencyModInvert: Boolean = false
    )

    data class MainOptionsState (
        val channelAPower: Int = 0,
        val channelBPower: Int = 0,
        val globalMute: Boolean = false,
        val swapChannels: Boolean = false,
        val frequencyRange: ClosedFloatingPointRange<Float> = 10f..100f
    )

    data class MiscOptionsState (
        val powerStepSize: Int = 1,
    )
}