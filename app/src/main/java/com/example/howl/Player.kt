package com.example.howl

import android.content.Context
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

fun formatTime(position: Double): String {
    val minutes = (position / 60).toInt()
    val seconds = position % 60
    return String.format(Locale.US, "%02d:%04.1f", minutes, seconds)
}

object Player {
    var reader: FileReader = FileReader()
    var shouldLoop: Boolean = false

    fun updatePlayerState(newPlayerState: DataRepository.FunscriptPlayerState) {
        DataRepository.setFunscriptPlayerState(newPlayerState)
    }
    fun updateAdvancedControlsState(newAdvancedControlsState: DataRepository.FunscriptAdvancedControlsState) {
        DataRepository.setFunscriptAdvancedControlsState(newAdvancedControlsState)
    }
    fun getPulseAtTime(time: Double): Pulse {
        return reader.getPulseAtTime(time)
    }
    fun stopPlayer() {
        updatePlayerState(DataRepository.funscriptPlayerState.value.copy(isPlaying = false))
    }
    fun startPlayer(from: Double = 0.0) {
        if(!reader.readyToPlay)
            return
        updatePlayerState(DataRepository.funscriptPlayerState.value.copy(isPlaying = true, startTime = markNow(), startPosition = from))
    }
    fun loadFile(uri: Uri, context: Context) {
        try {
            reader = HWLReader()
            val length = reader.open(uri, context)
            shouldLoop = true
            updatePlayerState(DataRepository.FunscriptPlayerState(filename = reader.filename, fileLength = length))
            return
        }
        catch (_: BadFileException) { }
        try {
            reader = FunscriptReader()
            val length = reader.open(uri, context)
            shouldLoop = false
            updatePlayerState(DataRepository.FunscriptPlayerState(filename = reader.filename, fileLength = length))
            return
        }
        catch (_: BadFileException) { }
        reader = FileReader()
        updatePlayerState(DataRepository.FunscriptPlayerState(filename = reader.filename, fileLength = 0.0))
    }
    fun getCurrentPosition(): Double {
        val playerState = DataRepository.funscriptPlayerState.value
        return playerState.startPosition + playerState.startTime!!.elapsedNow()
            .toDouble(
                DurationUnit.SECONDS
            )
    }
    fun setCurrentPosition(position: Double) {
        DataRepository.setFunscriptPlayerPosition(position)
    }
}

class PlayerViewModel() : ViewModel() {
    val playerState: StateFlow<DataRepository.FunscriptPlayerState> = DataRepository.funscriptPlayerState
    val advancedControlsState: StateFlow<DataRepository.FunscriptAdvancedControlsState> =
        DataRepository.funscriptAdvancedControlsState

    fun updatePlayerState(newPlayerState: DataRepository.FunscriptPlayerState) {
        DataRepository.setFunscriptPlayerState(newPlayerState)
    }

    fun updateAdvancedControlsState(newAdvancedControlsState: DataRepository.FunscriptAdvancedControlsState) {
        DataRepository.setFunscriptAdvancedControlsState(newAdvancedControlsState)
    }

    fun stopPlayer() {
        Player.stopPlayer()
    }

    fun startPlayer(from: Double = 0.0) {
        Player.startPlayer(from)
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
            Text(text = "Funscript settings", style = MaterialTheme.typography.headlineSmall)
        }
        SliderWithLabel(
            label = "Channel bias factor",
            value = advancedControlsState.channelBiasFactor,
            onValueChange = {viewModel.updateAdvancedControlsState(advancedControlsState.copy(channelBiasFactor = it))},
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0f..1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
        SliderWithLabel(
            label = "Frequency separation factor",
            value = advancedControlsState.frequencySeparation,
            onValueChange = {viewModel.updateAdvancedControlsState(advancedControlsState.copy(frequencySeparation = it))},
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = 0f..1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%03.2f", it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Invert channel A frequencies", style = MaterialTheme.typography.labelLarge)
            Switch(
                checked = advancedControlsState.frequencyInversion,
                onCheckedChange = {
                    viewModel.updateAdvancedControlsState(
                        advancedControlsState.copy(
                            frequencyInversion = it
                        )
                    )
                    viewModel.saveSettings()
                }
            )
        }
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
        if(playerState.filename != null)
            Text(text = "${playerState.filename}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
        else
            Text(text = "Funscript/HWL player", style = MaterialTheme.typography.labelLarge)

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Position Display
            Text(text = formatTime(playerState.currentPosition))
            Spacer(modifier = Modifier.width(4.dp))
            // Seek Bar
            Slider(
                value = playerState.currentPosition.toFloat(),
                onValueChange = { newValue ->
                    viewModel.updatePlayerState(playerState.copy(currentPosition = newValue.toDouble()))
                },
                valueRange = 0f..playerState.fileLength.toFloat(),
                onValueChangeFinished = { }
            )
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
                        viewModel.startPlayer(playerState.currentPosition)
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