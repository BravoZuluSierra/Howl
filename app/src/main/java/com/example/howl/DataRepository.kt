package com.example.howl

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.TimeMark

object DataRepository {
    var database: HowlDatabase? = null

    private val _funscriptPlayerState = MutableStateFlow(FunscriptPlayerState())
    val funscriptPlayerState: StateFlow<FunscriptPlayerState> = _funscriptPlayerState.asStateFlow()

    private val _funscriptAdvancedControlsState = MutableStateFlow(FunscriptAdvancedControlsState())
    val funscriptAdvancedControlsState: StateFlow<FunscriptAdvancedControlsState> = _funscriptAdvancedControlsState.asStateFlow()

    private val _mainOptionsState = MutableStateFlow(MainOptionsState())
    val mainOptionsState: StateFlow<MainOptionsState> = _mainOptionsState.asStateFlow()

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

    fun setCoyoteBatteryLevel(percent: Int) {
        _coyoteBatteryLevel.update { percent }
    }

    fun setCoyoteConnectionStatus(status: ConnectionStatus) {
        _coyoteConnectionStatus.update { status }
    }

    fun setChannelAPower(power: Int) {
        if(power > coyoteParametersState.value.channelALimit)
            return
        _mainOptionsState.update { it.copy(channelAPower = power)}
    }

    fun setChannelBPower(power: Int) {
        if(power > coyoteParametersState.value.channelBLimit)
            return
        _mainOptionsState.update { it.copy(channelBPower = power)}
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
            frequencyInversion = funscriptAdvancedControlsState.value.frequencyInversion,
            frequencySeparation = funscriptAdvancedControlsState.value.frequencySeparation,
            channelBiasFactor = funscriptAdvancedControlsState.value.channelBiasFactor,
            autoCycle = generatorState.value.autoCycle,
            autoCycleTime = generatorState.value.autoCycleTime
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
            frequencyInversion = settings.frequencyInversion,
            channelBiasFactor = settings.channelBiasFactor,
            frequencySeparation = settings.frequencySeparation
        ))
        setGeneratorState(generatorState.value.copy(
            autoCycle = settings.autoCycle,
            autoCycleTime = settings.autoCycleTime
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
        val frequencyInversion: Boolean = false,
        val channelBiasFactor: Float = 0.7f,
        val frequencySeparation: Float = 0.1f
    )

    data class MainOptionsState (
        val channelAPower: Int = 0,
        val channelBPower: Int = 0,
        val globalMute: Boolean = false,
        val swapChannels: Boolean = false,
        val frequencyRange: ClosedFloatingPointRange<Float> = 10f..100f
    )
}