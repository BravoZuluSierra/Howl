package com.example.howl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.howl.ui.theme.HowlTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.TimeSource.Monotonic.markNow
import java.util.Locale
import kotlin.math.pow
import java.lang.ref.WeakReference

fun formatTime(position: Double): String {
    val minutes = (position / 60).toInt()
    val seconds = position % 60
    return String.format(Locale.US, "%02d:%04.1f", minutes, seconds)
}

interface PulseSource {
    val displayName: String
    val duration: Double?
    val isFinite: Boolean
    val shouldLoop: Boolean
    val readyToPlay: Boolean
    fun getPulseAtTime(time: Double): Pulse
    fun updateState(currentTime: Double)
}

object Player {
    private var contextRef: WeakReference<Context>? = null
    val mainTimerDelay: Double = 0.1
    val pulseBatchSize: Int = 4
    val pulseTime = mainTimerDelay / pulseBatchSize
    private var autoIncrementPowerCounterA: Int = 0
    private var autoIncrementPowerCounterB: Int = 0

    fun initialise(context: Context) {
        this.contextRef = WeakReference(context)
    }
    fun getNextTimes(time: Double): List<Double> {
        val playbackSpeed = DataRepository.playerAdvancedControlsState.value.playbackSpeed
        val relativePulseTime = pulseTime * playbackSpeed
        return List(pulseBatchSize) { index ->
            time + relativePulseTime * index.toDouble()
        }
    }
    fun updatePlayerState(newPlayerState: DataRepository.PlayerState) {
        DataRepository.setPlayerState(newPlayerState)
    }
    fun updateAdvancedControlsState(newAdvancedControlsState: DataRepository.PlayerAdvancedControlsState) {
        DataRepository.setPlayerAdvancedControlsState(newAdvancedControlsState)
    }
    fun applyPostProcessing(time: Double, pulse: Pulse): Pulse {
        val mainOptionsState = DataRepository.mainOptionsState.value
        val advancedControlState = DataRepository.playerAdvancedControlsState.value
        val developerOptionsState = DataRepository.developerOptionsState.value
        var newFreqA = pulse.freqA
        var newFreqB = pulse.freqB
        var newAmpA = pulse.ampA
        var newAmpB = pulse.ampB
        if (advancedControlState.frequencyInversionA)
            newFreqA = 1 - pulse.freqA
        if (advancedControlState.frequencyInversionB)
            newFreqB = 1 - pulse.freqB
        if (showDeveloperOptions) {
            newAmpA =
                (newAmpA.pow(developerOptionsState.developerAmplitudeExponent) * developerOptionsState.developerAmplitudeGain).coerceIn(0f, 1f)
            newAmpB =
                (newAmpB.pow(developerOptionsState.developerAmplitudeExponent) * developerOptionsState.developerAmplitudeGain).coerceIn(0f, 1f)
            newFreqA =
                (newFreqA.pow(developerOptionsState.developerFrequencyExponent) * developerOptionsState.developerFrequencyGain).coerceIn(0f, 1f)
            newFreqB =
                (newFreqB.pow(developerOptionsState.developerFrequencyExponent) * developerOptionsState.developerFrequencyGain).coerceIn(0f, 1f)
            newFreqA = (newFreqA + developerOptionsState.developerFrequencyAdjustA).coerceIn(0f, 1f)
            newFreqB = (newFreqB + developerOptionsState.developerFrequencyAdjustB).coerceIn(0f, 1f)
        }

        val swapChannels = mainOptionsState.swapChannels
        return if(swapChannels)
            (Pulse(ampA = newAmpB, ampB = newAmpA, freqA = newFreqB, freqB = newFreqA))
        else
            (Pulse(ampA = newAmpA, ampB = newAmpB, freqA = newFreqA, freqB = newFreqB))
    }
    fun getPulseAtTime(time: Double): Pulse {
        val activePulseSource = DataRepository.playerState.value.activePulseSource
        val pulse = activePulseSource?.getPulseAtTime(time) ?: Pulse()
        return applyPostProcessing(time, pulse)
    }
    fun stopPlayer() {
        updatePlayerState(DataRepository.playerState.value.copy(isPlaying = false))
    }
    fun startPlayer(from: Double? = null) {
        val playerState = DataRepository.playerState.value
        val playFrom = from ?: playerState.currentPosition
        if(playerState.activePulseSource?.readyToPlay != true)
            return
        updatePlayerState(playerState.copy(isPlaying = true, startTime = markNow(), startPosition = playFrom))

        val context = contextRef?.get() ?: return
        val serviceIntent = Intent(context, PlayerService::class.java)
        context.startService(serviceIntent)
    }
    fun loadFile(uri: Uri, context: Context) {
        DataRepository.setPlayerPosition(0.0)
        try {
            val pulseSource = HWLPulseSource()
            pulseSource.open(uri, context)
            switchPulseSource(pulseSource)
            return
        }
        catch (_: BadFileException) { }
        try {
            val pulseSource = FunscriptPulseSource()
            pulseSource.open(uri, context)
            switchPulseSource(pulseSource)
            return
        }
        catch (_: BadFileException) { }
        switchPulseSource(null)
    }
    fun switchPulseSource(source: PulseSource?) {
        val playerState = DataRepository.playerState.value
        if (source == null || playerState.activePulseSource != source) {
            updatePlayerState(DataRepository.PlayerState())
            DataRepository.setPlayerPulseSource(source)
        }
    }
    fun getCurrentPosition(): Double {
        val playerState = DataRepository.playerState.value
        val playbackSpeed = DataRepository.playerAdvancedControlsState.value.playbackSpeed
        val elapsed = playerState.startTime?.elapsedNow()?.toDouble(DurationUnit.SECONDS) ?: 0.0
        return playerState.startPosition + elapsed * playbackSpeed
    }
    fun handlePowerAutoIncrement() {
        val mainOptionsState = DataRepository.mainOptionsState.value
        val miscOptionsState = DataRepository.miscOptionsState.value

        if (mainOptionsState.autoIncreasePower == true && mainOptionsState.globalMute == false) {
            if (mainOptionsState.channelAPower > 0)
                autoIncrementPowerCounterA++
            if (mainOptionsState.channelBPower > 0)
                autoIncrementPowerCounterB++

            val autoIncrementDelayA =
                miscOptionsState.powerAutoIncrementDelayA
            val autoIncrementDelayB =
                miscOptionsState.powerAutoIncrementDelayB
            //Log.d("Player", "Auto increment calculation $autoIncrementPowerCounterA / $autoIncrementDelayA      $autoIncrementPowerCounterB / $autoIncrementDelayB")
            if (autoIncrementPowerCounterA >= autoIncrementDelayA * 10) {
                autoIncrementPowerCounterA = 0
                DataRepository.setChannelAPower(mainOptionsState.channelAPower + 1)
            }
            if (autoIncrementPowerCounterB >= autoIncrementDelayB * 10) {
                autoIncrementPowerCounterB = 0
                DataRepository.setChannelBPower(mainOptionsState.channelBPower + 1)
            }
        }
    }
    fun setCurrentPosition(position: Double) {
        DataRepository.setPlayerPosition(position)
    }
}

class PlayerViewModel() : ViewModel() {
    val playerState: StateFlow<DataRepository.PlayerState> = DataRepository.playerState
    val advancedControlsState: StateFlow<DataRepository.PlayerAdvancedControlsState> =
        DataRepository.playerAdvancedControlsState

    fun updatePlayerState(newPlayerState: DataRepository.PlayerState) {
        DataRepository.setPlayerState(newPlayerState)
    }

    fun updateAdvancedControlsState(newAdvancedControlsState: DataRepository.PlayerAdvancedControlsState) {
        DataRepository.setPlayerAdvancedControlsState(newAdvancedControlsState)
    }

    fun stopPlayer() {
        Player.stopPlayer()
    }

    fun startPlayer(from: Double? = null) {
        Player.startPlayer(from)
    }

    fun seek(position: Double) {
        val playerState = DataRepository.playerState.value
        val finite = playerState.activePulseSource?.isFinite
        if (finite == true) {
            Player.updatePlayerState(
                playerState.copy(
                    currentPosition = position,
                )
            )
            if (playerState.isPlaying)
                resyncPlayer(position)
        }
    }

    fun resyncPlayer(position: Double? = null) {
        val playerState = DataRepository.playerState.value
        val pos = position ?: playerState.currentPosition

        if (playerState.isPlaying) {
            Player.updatePlayerState(
                playerState.copy(
                    startTime = markNow(),
                    startPosition = pos
                )
            )
        }
    }

    fun loadFile(uri: Uri, context: Context) {
        Player.loadFile(uri, context)
    }

    fun saveSettings() {
        viewModelScope.launch {
            DataRepository.saveSettings()
        }
    }
}

@Composable
fun AdvancedControlsPanel(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val advancedControlsState by viewModel.advancedControlsState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Global settings", style = MaterialTheme.typography.headlineSmall)
        }
        SliderWithLabel(
            label = "Playback speed",
            value = advancedControlsState.playbackSpeed,
            onValueChange = {
                viewModel.updateAdvancedControlsState(advancedControlsState.copy(playbackSpeed = it))
                viewModel.resyncPlayer()
            },
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0.25f..4.0f,
            steps = 14,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Invert channel A frequencies", style = MaterialTheme.typography.labelLarge)
            Switch(
                checked = advancedControlsState.frequencyInversionA,
                onCheckedChange = {
                    viewModel.updateAdvancedControlsState(
                        advancedControlsState.copy(
                            frequencyInversionA = it
                        )
                    )
                    viewModel.saveSettings()
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Invert channel B frequencies", style = MaterialTheme.typography.labelLarge)
            Switch(
                checked = advancedControlsState.frequencyInversionB,
                onCheckedChange = {
                    viewModel.updateAdvancedControlsState(
                        advancedControlsState.copy(
                            frequencyInversionB = it
                        )
                    )
                    viewModel.saveSettings()
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Funscript settings", style = MaterialTheme.typography.headlineSmall)
        }
        SliderWithLabel(
            label = "Volume (versus dynamic range)",
            value = advancedControlsState.funscriptVolume,
            onValueChange = {viewModel.updateAdvancedControlsState(advancedControlsState.copy(funscriptVolume = it))},
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0f..1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
        SliderWithLabel(
            label = "Positional effect strength",
            value = advancedControlsState.funscriptPositionalEffectStrength,
            onValueChange = {viewModel.updateAdvancedControlsState(advancedControlsState.copy(funscriptPositionalEffectStrength = it))},
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0f..1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
        SliderWithLabel(
            label = "Feel adjustment",
            value = advancedControlsState.funscriptFeel,
            onValueChange = {viewModel.updateAdvancedControlsState(advancedControlsState.copy(funscriptFeel = it))},
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0.5f..2.0f,
            steps = 149,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
        SliderWithLabel(
            label = "A/B frequency time offset",
            value = advancedControlsState.funscriptFrequencyTimeOffset,
            onValueChange = {viewModel.updateAdvancedControlsState(advancedControlsState.copy(funscriptFrequencyTimeOffset = it))},
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0f..1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
    }
}

@Composable
fun PlayerPanel(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdvancedSettings by remember { mutableStateOf(false) }

    // Temporary variables that we use to ensure the player position only gets
    // updated once the user finishes dragging the drag handle on the seek bar.
    // This prevents sending garbled output when the user drags during playback.
    var isDragging by remember { mutableStateOf(false) }
    var tempPosition by remember { mutableDoubleStateOf(playerState.currentPosition) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.loadFile(uri, context)
            }
        }
    )

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // File Name Display
        val displayName = playerState.activePulseSource?.displayName ?: "Player"
        val duration = playerState.activePulseSource?.duration ?: 0.0
        Text(text = displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Position Display
            val pos = if (isDragging) tempPosition else playerState.currentPosition
            Text(text = formatTime(pos))
            Spacer(modifier = Modifier.width(4.dp))

            // Seek Bar
            Slider(
                value = pos.toFloat(),
                onValueChange = {
                    tempPosition = it.toDouble()
                    if (!isDragging) isDragging = true
                },
                valueRange = 0f..duration.toFloat(),
                onValueChangeFinished = {
                    isDragging = false
                    viewModel.seek(tempPosition)
                }
            )
            /*Slider(
                value = playerState.currentPosition.toFloat(),
                onValueChange = { viewModel.seek(it.toDouble()) },
                valueRange = 0f..duration.toFloat(),
                onValueChangeFinished = { }
            )*/
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Play/Pause Button
            Button(
                onClick = {
                    if (playerState.isPlaying)
                        viewModel.stopPlayer()
                    else
                        viewModel.startPlayer()
                }
            ) {
                if (playerState.isPlaying) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = "Pause"
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = "Play"
                    )
                }
            }
            // File Picker Button
            Button(
                onClick = {
                    viewModel.stopPlayer()
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.folder_open),
                    contentDescription = "Open file"
                )
            }
            // Advanced options button
            Button(
                onClick = {
                    showAdvancedSettings = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Advanced settings"
                )
            }
        }

        if (showAdvancedSettings) {
            Dialog(
                onDismissRequest = { showAdvancedSettings = false }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    AdvancedControlsPanel(
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PlayerPreview() {
    HowlTheme {
        val viewModel: PlayerViewModel = viewModel()
        PlayerPanel(
            viewModel = viewModel,
            modifier = Modifier.fillMaxHeight()
        )
    }
}