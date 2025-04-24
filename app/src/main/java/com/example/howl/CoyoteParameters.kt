package com.example.howl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.howl.ui.theme.HowlTheme
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Locale

fun IntRange.toClosedFloatingPointRange(): ClosedFloatingPointRange<Float> {
    return this.start.toFloat()..this.endInclusive.toFloat()
}

class CoyoteParametersViewModel() : ViewModel() {
    val developerExponentRange: ClosedFloatingPointRange<Float> = 0.5f .. 2.0f
    val developerGainRange: ClosedFloatingPointRange<Float> = 0.5f .. 2.0f
    val developerFrequencyAdjustRange: ClosedFloatingPointRange<Float> = -1.0f .. 1.0f
    val coyoteParametersState: StateFlow<DGCoyote.Parameters> = DataRepository.coyoteParametersState
    val miscOptionsState: StateFlow<DataRepository.MiscOptionsState> = DataRepository.miscOptionsState
    val developerOptionsState: StateFlow<DataRepository.DeveloperOptionsState> = DataRepository.developerOptionsState
    fun setCoyoteParametersState(newCoyoteParametersState: DGCoyote.Parameters) {
        DataRepository.setCoyoteParametersState(newCoyoteParametersState)
    }
    fun setMiscOptionsState(newMiscOptionsState: DataRepository.MiscOptionsState) {
        DataRepository.setMiscOptionsState(newMiscOptionsState)
    }
    fun setDeveloperOptionsState(newDeveloperOptionsState: DataRepository.DeveloperOptionsState) {
        DataRepository.setDeveloperOptionsState(newDeveloperOptionsState)
    }
    fun syncParameters() {
        viewModelScope.launch {
            DataRepository.saveSettings()
        }
        DGCoyote.sendParameters(DataRepository.coyoteParametersState.value)
    }
    fun saveSettings() {
        viewModelScope.launch {
            DataRepository.saveSettings()
        }
    }
}

@Composable
fun CoyoteParametersPanel(
    viewModel: CoyoteParametersViewModel,
    modifier: Modifier = Modifier
) {
    val parametersState by viewModel.coyoteParametersState.collectAsStateWithLifecycle()
    val miscOptionsState by viewModel.miscOptionsState.collectAsStateWithLifecycle()
    val developerOptionsState by viewModel.developerOptionsState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Coyote parameters", style = MaterialTheme.typography.headlineSmall)
        }

        SliderWithLabel(
            label = "Channel A Power Limit",
            value = parametersState.channelALimit.toFloat(),
            onValueChange = { viewModel.setCoyoteParametersState(parametersState.copy(channelALimit = it.roundToInt())) },
            onValueChangeFinished = { viewModel.syncParameters() },
            valueRange = DGCoyote.POWER_RANGE.toClosedFloatingPointRange(),
            steps = DGCoyote.POWER_RANGE.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )

        SliderWithLabel(
            label = "Channel B Power Limit",
            value = parametersState.channelBLimit.toFloat(),
            onValueChange = { viewModel.setCoyoteParametersState(parametersState.copy(channelBLimit = it.roundToInt())) },
            onValueChangeFinished = { viewModel.syncParameters() },
            valueRange = DGCoyote.POWER_RANGE.toClosedFloatingPointRange(),
            steps = DGCoyote.POWER_RANGE.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )

        SliderWithLabel(
            label = "Channel A Frequency Balance",
            value = parametersState.channelAFrequencyBalance.toFloat(),
            onValueChange = { viewModel.setCoyoteParametersState(parametersState.copy(channelAFrequencyBalance = it.roundToInt())) },
            onValueChangeFinished = { viewModel.syncParameters() },
            valueRange = DGCoyote.FREQUENCY_BALANCE_RANGE.toClosedFloatingPointRange(),
            steps = DGCoyote.FREQUENCY_BALANCE_RANGE.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )

        SliderWithLabel(
            label = "Channel B Frequency Balance",
            value = parametersState.channelBFrequencyBalance.toFloat(),
            onValueChange = { viewModel.setCoyoteParametersState(parametersState.copy(channelBFrequencyBalance = it.roundToInt())) },
            onValueChangeFinished = { viewModel.syncParameters() },
            valueRange = DGCoyote.FREQUENCY_BALANCE_RANGE.toClosedFloatingPointRange(),
            steps = DGCoyote.FREQUENCY_BALANCE_RANGE.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )

        SliderWithLabel(
            label = "Channel A Intensity Balance",
            value = parametersState.channelAIntensityBalance.toFloat(),
            onValueChange = { viewModel.setCoyoteParametersState(parametersState.copy(channelAIntensityBalance = it.roundToInt())) },
            onValueChangeFinished = { viewModel.syncParameters() },
            valueRange = DGCoyote.INTENSITY_BALANCE_RANGE.toClosedFloatingPointRange(),
            steps = DGCoyote.INTENSITY_BALANCE_RANGE.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )

        SliderWithLabel(
            label = "Channel B Intensity Balance",
            value = parametersState.channelBIntensityBalance.toFloat(),
            onValueChange = { viewModel.setCoyoteParametersState(parametersState.copy(channelBIntensityBalance = it.roundToInt())) },
            onValueChangeFinished = { viewModel.syncParameters() },
            valueRange = DGCoyote.INTENSITY_BALANCE_RANGE.toClosedFloatingPointRange(),
            steps = DGCoyote.INTENSITY_BALANCE_RANGE.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Misc options", style = MaterialTheme.typography.headlineSmall)
        }
        val powerStepRange: IntRange = 1..10
        SliderWithLabel(
            label = "Power control step size A",
            value = miscOptionsState.powerStepSizeA.toFloat(),
            onValueChange = { viewModel.setMiscOptionsState(miscOptionsState.copy(powerStepSizeA = it.roundToInt())) },
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = powerStepRange.toClosedFloatingPointRange(),
            steps = powerStepRange.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )
        SliderWithLabel(
            label = "Power control step size B",
            value = miscOptionsState.powerStepSizeB.toFloat(),
            onValueChange = { viewModel.setMiscOptionsState(miscOptionsState.copy(powerStepSizeB = it.roundToInt())) },
            onValueChangeFinished = { viewModel.saveSettings() },
            valueRange = powerStepRange.toClosedFloatingPointRange(),
            steps = powerStepRange.endInclusive - 1,
            valueDisplay = { it.roundToInt().toString() }
        )
        if (showDeveloperOptions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Developer options", style = MaterialTheme.typography.headlineSmall)
            }
            SliderWithLabel(
                label = "Frequency exponent",
                value = developerOptionsState.developerFrequencyExponent.toFloat(),
                onValueChange = { viewModel.setDeveloperOptionsState(developerOptionsState.copy(developerFrequencyExponent = it)) },
                onValueChangeFinished = { },
                valueRange = viewModel.developerExponentRange,
                steps = ((viewModel.developerExponentRange.endInclusive - viewModel.developerExponentRange.start) * 10.0 - 1).roundToInt(),
                valueDisplay = { String.format(Locale.US, "%02.1f", it) }
            )
            SliderWithLabel(
                label = "Frequency gain",
                value = developerOptionsState.developerFrequencyGain.toFloat(),
                onValueChange = { viewModel.setDeveloperOptionsState(developerOptionsState.copy(developerFrequencyGain = it)) },
                onValueChangeFinished = { },
                valueRange = viewModel.developerGainRange,
                steps = ((viewModel.developerGainRange.endInclusive - viewModel.developerGainRange.start) * 10.0 - 1).roundToInt(),
                valueDisplay = { String.format(Locale.US, "%02.1f", it) }
            )
            SliderWithLabel(
                label = "Channel A frequency adjust",
                value = developerOptionsState.developerFrequencyAdjustA.toFloat(),
                onValueChange = { viewModel.setDeveloperOptionsState(developerOptionsState.copy(developerFrequencyAdjustA = it)) },
                onValueChangeFinished = { },
                valueRange = viewModel.developerFrequencyAdjustRange,
                steps = ((viewModel.developerFrequencyAdjustRange.endInclusive - viewModel.developerFrequencyAdjustRange.start) * 20.0 - 1).roundToInt(),
                valueDisplay = { String.format(Locale.US, "%03.2f", it) }
            )
            SliderWithLabel(
                label = "Channel B frequency adjust",
                value = developerOptionsState.developerFrequencyAdjustB.toFloat(),
                onValueChange = { viewModel.setDeveloperOptionsState(developerOptionsState.copy(developerFrequencyAdjustB = it)) },
                onValueChangeFinished = { },
                valueRange = viewModel.developerFrequencyAdjustRange,
                steps = ((viewModel.developerFrequencyAdjustRange.endInclusive - viewModel.developerFrequencyAdjustRange.start) * 20.0 - 1).roundToInt(),
                valueDisplay = { String.format(Locale.US, "%03.2f", it) }
            )
            SliderWithLabel(
                label = "Amplitude exponent",
                value = developerOptionsState.developerAmplitudeExponent.toFloat(),
                onValueChange = { viewModel.setDeveloperOptionsState(developerOptionsState.copy(developerAmplitudeExponent = it)) },
                onValueChangeFinished = { },
                valueRange = viewModel.developerExponentRange,
                steps = ((viewModel.developerExponentRange.endInclusive - viewModel.developerExponentRange.start) * 10.0 - 1).roundToInt(),
                valueDisplay = { String.format(Locale.US, "%02.1f", it) }
            )
            SliderWithLabel(
                label = "Amplitude gain",
                value = developerOptionsState.developerAmplitudeGain.toFloat(),
                onValueChange = { viewModel.setDeveloperOptionsState(developerOptionsState.copy(developerAmplitudeGain = it)) },
                onValueChangeFinished = { },
                valueRange = viewModel.developerGainRange,
                steps = ((viewModel.developerGainRange.endInclusive - viewModel.developerGainRange.start) * 10.0 - 1).roundToInt(),
                valueDisplay = { String.format(Locale.US, "%02.1f", it) }
            )
        }
    }
}

@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Column( modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),//.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = valueDisplay(value),
                modifier = Modifier.widthIn(45.dp)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Preview
@Composable
fun CoyoteParametersPanelPreview() {
    HowlTheme {
        val viewModel: CoyoteParametersViewModel = viewModel()
        CoyoteParametersPanel(
            viewModel = viewModel,
            modifier = Modifier.fillMaxHeight()
        )
    }
}