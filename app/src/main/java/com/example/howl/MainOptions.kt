package com.example.howl
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.howl.ui.theme.HowlTheme
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

class MainOptionsViewModel() : ViewModel() {
    val mainOptionsState: StateFlow<DataRepository.MainOptionsState> = DataRepository.mainOptionsState
    val miscOptionsState: StateFlow<DataRepository.MiscOptionsState> = DataRepository.miscOptionsState

    fun updateMainOptionsState(newMainOptionsState: DataRepository.MainOptionsState) {
        DataRepository.setMainOptionsState(newMainOptionsState)
    }

    fun setChannelAPower(power: Int) {
        DataRepository.setChannelAPower(power)
    }

    fun setChannelBPower(power: Int) {
        DataRepository.setChannelBPower(power)
    }

    fun setGlobalMute(muted: Boolean) {
        DataRepository.setGlobalMute(muted)
    }

    fun setSwapChannels(swap: Boolean) {
        DataRepository.setSwapChannels(swap)
    }

    fun setFrequencyRange(range: ClosedFloatingPointRange<Float>) {
        DataRepository.setFrequencyRange(range)
    }

    fun cyclePulseChart() {
        val newMode = mainOptionsState.value.pulseChartMode.next()
        DataRepository.setPulseChartMode(newMode)
        if (newMode == PulseChartMode.Off)
            DataRepository.clearPulseHistory()
    }
}

@Composable
fun MainOptionsPanel(
    viewModel: MainOptionsViewModel,
    modifier: Modifier = Modifier
) {
    val mainOptionsState by viewModel.mainOptionsState.collectAsStateWithLifecycle()
    val miscOptionsState by viewModel.miscOptionsState.collectAsStateWithLifecycle()
    val minSeparation = 5f
    val muted = mainOptionsState.globalMute
    val swapChannels = mainOptionsState.swapChannels

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PowerLevelPanel(
                title = "Channel A Power",
                power = mainOptionsState.channelAPower,
                onPowerChange = viewModel::setChannelAPower,
                stepSize = miscOptionsState.powerStepSizeA
            )
            PowerLevelPanel(
                title = "Channel B Power",
                power = mainOptionsState.channelBPower,
                onPowerChange = viewModel::setChannelBPower,
                stepSize = miscOptionsState.powerStepSizeB
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                modifier = Modifier.weight(1.0f)
                    .height(50.dp),
                onClick = {
                    viewModel.setGlobalMute(!muted)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (muted) Color.Red else ButtonDefaults.buttonColors().containerColor
                )
            ) {
                Text(if (muted) "Pulse output muted" else "Mute pulse output")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.height(50.dp),
                onClick = {
                    viewModel.cyclePulseChart()
                },
            ) {
                Icon(painter = painterResource(R.drawable.chart), contentDescription = "Pulse chart")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.height(50.dp),
                onClick = {
                    viewModel.setSwapChannels(!swapChannels)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (swapChannels) Color.Red else ButtonDefaults.buttonColors().containerColor
                )
            ) {
                Icon(painter = painterResource(R.drawable.swap), contentDescription = "Swap channels")
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Frequency range (Hz)", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${mainOptionsState.frequencyRange.start.roundToInt()}", modifier = modifier.widthIn(30.dp))
            RangeSlider(
                modifier = Modifier.weight(1f),
                value = mainOptionsState.frequencyRange,
                steps = 0, //there's a crash due to an Android bug if we set the steps we actually want
                //steps = (DGCoyote.FREQUENCY_RANGE.endInclusive - DGCoyote.FREQUENCY_RANGE.start) - 1,
                //make it continuous and round to integer values in onValueChange instead as a workaround
                onValueChange = { newRange ->
                    val newStart = newRange.start
                    val newEnd = newRange.endInclusive
                    if (newEnd - newStart >= minSeparation) {
                        viewModel.setFrequencyRange(
                            newRange.start.roundToInt()
                                .toFloat()..newRange.endInclusive.roundToInt().toFloat()
                        )
                    }
                },
                valueRange = DGCoyote.FREQUENCY_RANGE.toClosedFloatingPointRange(),
                onValueChangeFinished = { },
            )
            Text(text = "${mainOptionsState.frequencyRange.endInclusive.roundToInt()}", modifier = modifier.widthIn(30.dp))
        }
        when (mainOptionsState.pulseChartMode) {
            PulseChartMode.Combined -> {
                PulsePlotter(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(3.dp),
                    mode = PulsePlotMode.Combined
                )
            }
            PulseChartMode.Separate -> {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PulsePlotter(
                        modifier = Modifier
                            .height(200.dp)
                            .weight(1.0f)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(3.dp),
                        mode = PulsePlotMode.AmplitudeOnly
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PulsePlotter(
                        modifier = Modifier
                            .height(200.dp)
                            .weight(1.0f)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(3.dp),
                        mode = PulsePlotMode.FrequencyOnly
                    )
                }
            }
            PulseChartMode.Off -> {}
        }
    }
}

@Composable
fun PowerLevelPanel(
    title: String,
    power: Int,
    onPowerChange: (Int) -> Unit,
    stepSize: Int
) {
    Column {
        Text(text = title, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(
            text = "$power",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row {
            Button(
                onClick = { onPowerChange(maxOf(0, power - stepSize)) },
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painter = painterResource(R.drawable.minus), contentDescription = "Lower power")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onPowerChange(power + stepSize) },
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painter = painterResource(R.drawable.plus), contentDescription = "Increase power")
            }
        }
    }
}

@Preview
@Composable
fun MainOptionsPanelPreview() {
    HowlTheme {
        val viewModel: MainOptionsViewModel = viewModel()
        MainOptionsPanel(
            viewModel = viewModel,
            modifier = Modifier.fillMaxHeight()
        )
    }
}