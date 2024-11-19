package com.example.howl

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.TimeSource.Monotonic.markNow

enum class WaveType(val description: String) {
    Sine("Sine"),
    Cosine("Cosine"),
    Sawtooth("Sawtooth"),
    Triangle("Triangle"),
    Square("Square"),
    Constant("Constant"),
    Trapezium("Trapezium"),
    Fangs("Fangs"),
}

data class GeneratorParameters(
    var waveType: WaveType = WaveType.Sine,
    var frequencyType: WaveType = WaveType.Sine,
    var period: Double = 1.0,
    var frequencyHigh: Double = 0.5,
    var frequencyLow: Double = 0.0,
    var amplitudeHigh: Double = 0.5,
    var amplitudeLow: Double = 0.0,
    var timeOffset: Double = 0.0,
)

data class Point(
    val time: Double,
    val position: Double
)

object Generator {
    val AUTO_CYCLE_TIME_RANGE: IntRange = 10..300
    val PERIOD_RANGE: ClosedFloatingPointRange<Float> = 0.5f .. 10.0f
    fun initialise() {
        if(DataRepository.generatorState.value.initialised == false)
            randomise()
        updateGeneratorState(DataRepository.generatorState.value.copy(initialised = true))
    }
    fun updateGeneratorState(newGeneratorState: DataRepository.GeneratorState) {
        DataRepository.setGeneratorState(newGeneratorState)
    }
    fun stopGenerator() {
        updateGeneratorState(DataRepository.generatorState.value.copy(isPlaying = false))
    }
    fun startGenerator() {
        updateGeneratorState(DataRepository.generatorState.value.copy(isPlaying = true, patternStartTime = markNow()))
    }
    fun getCurrentTime(): Double {
        val generatorState = DataRepository.generatorState.value
        return generatorState.patternStartTime!!.elapsedNow().toDouble(DurationUnit.SECONDS)
    }
    private fun linearInterpolatePoints(time: Double, points: List<Point>) : Double {
        val index = points.indexOfFirst { it.time > time }
        if (index == 0)
            return points.first().position
        if (index == -1)
            return points.last().position
        val p1 = points[index - 1]
        val p2 = points[index]
        return linearInterpolate(time, p1, p2)
    }
    private fun linearInterpolate(time: Double, p1: Point, p2: Point): Double{
        return p1.position + (p2.position - p1.position) * (time - p1.time) / (p2.time - p1.time)
    }
    private fun calculateWave(wave: WaveType, relativeTime: Double, min: Double, max: Double): Double {
        val range: Double = abs(max - min)
        val mid: Double = (min + max) / 2.0

        return when (wave) {
            WaveType.Sine -> {
                mid + sin(Math.PI * 2.0 * relativeTime) * (range / 2.0)
            }
            WaveType.Cosine -> {
                mid + cos(Math.PI * 2.0 * relativeTime) * (range / 2.0)
            }
            WaveType.Sawtooth -> {
                linearInterpolate(relativeTime, Point(0.0, min), Point(1.0,max))
            }
            WaveType.Triangle -> {
                linearInterpolatePoints(relativeTime, listOf(
                    Point(0.0, min),
                    Point(0.5, max),
                    Point(1.0, min)))
            }
            WaveType.Square -> {
                if (relativeTime < 0.5) max else min
            }
            WaveType.Constant -> {
                max
            }
            WaveType.Trapezium -> {
                linearInterpolatePoints(relativeTime, listOf(
                    Point(0.0, min),
                    Point(0.4, max),
                    Point(0.6, max),
                    Point(1.0, min)))
            }
            WaveType.Fangs -> {
                linearInterpolatePoints(relativeTime, listOf(
                    Point(0.0, min),
                    Point(0.35, max),
                    Point(0.5, mid),
                    Point(0.65, max),
                    Point(1.0, min)))
            }
        }
    }
    private fun generate(time: Double, generatorParameters: GeneratorParameters, debug: Boolean = false): Pair<Float, Float> {
        val period = generatorParameters.period
        val calcTime = time + generatorParameters.timeOffset
        val iteration = (calcTime / period).toInt()
        val relativeTime = (calcTime % period) / period

        val frequency = calculateWave(generatorParameters.frequencyType, relativeTime, generatorParameters.frequencyLow, generatorParameters.frequencyHigh)
        val amplitude = calculateWave(generatorParameters.waveType, relativeTime, generatorParameters.amplitudeLow, generatorParameters.amplitudeHigh)

        if(debug)
            Log.d("Generator", "T=$time Iteration=$iteration Relative time=$relativeTime Frequency=$frequency Amplitude=$amplitude")
        return Pair(frequency.toFloat(), amplitude.toFloat())
    }
    fun getPulseAtTime(time: Double): Pulse {
        val (frequencyA, amplitudeA) = generate(time, DataRepository.generatorState.value.channelAParameters)//, debug = true)
        val (frequencyB, amplitudeB) = generate(time, DataRepository.generatorState.value.channelBParameters)
        return Pulse(amplitudeA, amplitudeB, frequencyA, frequencyB)
    }
    private fun getRandomGeneratorParameters(): GeneratorParameters {
        var period = Random.nextDouble()
        period *= if (Random.nextBoolean()) 2.5 else 9.5
        period += 0.5

        var amp1 = Random.nextDouble() * 0.4
        var amp2 = 0.5 + Random.nextDouble() * 0.5

        if (Random.nextBoolean())
            amp1 = amp2.also { amp2 = amp1 } //stupid Kotlin way to swap two variables

        var params: GeneratorParameters = GeneratorParameters (
            waveType = WaveType.entries.random(),
            frequencyType = WaveType.entries.random(),
            period = period,
            frequencyHigh = Random.nextDouble(),
            frequencyLow = Random.nextDouble(),
            amplitudeLow = amp1,
            amplitudeHigh = amp2
        )

        if (params.waveType == WaveType.Constant) {
            params.amplitudeLow = (Random.nextDouble() * 0.5) + 0.5
            params.amplitudeHigh = params.amplitudeLow
        }
        if (params.frequencyType == WaveType.Constant)
            params.frequencyHigh = params.frequencyLow

        return params
    }
    fun randomise() {
        var channelAParameters = getRandomGeneratorParameters()
        var channelBParameters = getRandomGeneratorParameters()
        if (Random.nextBoolean())
            channelBParameters.period = channelAParameters.period
        if (Random.nextBoolean())
            channelBParameters.timeOffset = Random.nextDouble() * (channelBParameters.period / 2.0)

        updateGeneratorState(DataRepository.generatorState.value.copy(
            channelAParameters = channelAParameters,
            channelBParameters = channelBParameters,
            patternStartTime = markNow()))
    }
}

class GeneratorViewModel() : ViewModel() {
    val generatorState: StateFlow<DataRepository.GeneratorState> = DataRepository.generatorState
    fun updateGeneratorState(newGeneratorState: DataRepository.GeneratorState) {
        DataRepository.setGeneratorState(newGeneratorState)
    }
    private fun getGeneratorParameters(channel: Int): GeneratorParameters {
        return if(channel == 1)
            DataRepository.generatorState.value.channelBParameters
        else
            DataRepository.generatorState.value.channelAParameters
    }
    private fun setGeneratorParameters(channel: Int, parameters: GeneratorParameters) {
        if(channel == 1)
            updateGeneratorState(DataRepository.generatorState.value.copy(channelBParameters = parameters))
        else
            updateGeneratorState(DataRepository.generatorState.value.copy(channelAParameters = parameters))
    }
    fun randomise() {
        Generator.randomise()
    }
    fun stopGenerator() {
        Generator.stopGenerator()
    }
    fun startGenerator() {
        Generator.startGenerator()
    }
    fun setAutoCycle(enabled: Boolean) {
        Generator.updateGeneratorState(DataRepository.generatorState.value.copy(autoCycle = enabled))
    }
    fun setAutoCycleTime(seconds: Int) {
        Generator.updateGeneratorState(DataRepository.generatorState.value.copy(autoCycleTime = seconds))
    }
    fun setPeriod(channel: Int, period: Double) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(period = period))
    }
    fun setMinPower(channel: Int, power: Double) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(amplitudeLow = power))
    }
    fun setMaxPower(channel: Int, power: Double) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(amplitudeHigh = power))
    }
    fun setMinFrequency(channel: Int, frequency: Double) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(frequencyLow = frequency))
    }
    fun setMaxFrequency(channel: Int, frequency: Double) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(frequencyHigh = frequency))
    }
    fun setWave(channel: Int, wave: WaveType) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(waveType = wave))
    }
    fun setFrequencyWave(channel: Int, wave: WaveType) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(frequencyType = wave))
    }
    fun setTimeOffset(channel: Int, seconds: Double) {
        setGeneratorParameters(channel, getGeneratorParameters(channel).copy(timeOffset = seconds))
    }

    fun saveSettings() {
        viewModelScope.launch {
            DataRepository.saveSettings()
        }
    }
}

@Composable
fun GeneratorPanel(
    viewModel: GeneratorViewModel,
    frequencyRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val generatorState by viewModel.generatorState.collectAsStateWithLifecycle()
    var showChannelASettings by remember { mutableStateOf(false) }
    var showChannelBSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GeneratorParametersInfo("Channel A", generatorState.channelAParameters, frequencyRange, onClick = { showChannelASettings = true }, modifier=Modifier.weight(1f))
            GeneratorParametersInfo("Channel B", generatorState.channelBParameters, frequencyRange, onClick = { showChannelBSettings = true }, modifier=Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Play/Pause Button
            Button(
                onClick = {
                    if (generatorState.isPlaying)
                        viewModel.stopGenerator()
                    else
                        viewModel.startGenerator()
                }
            ) {
                if (generatorState.isPlaying) {
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
            // Randomise button
            Button(
                onClick = { viewModel.randomise() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.casino),
                    contentDescription = "Randomise"
                )
                Text(text = "Random", modifier = Modifier.padding(start = 8.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Auto cycle patterns", style = MaterialTheme.typography.labelLarge)
            Switch(
                checked = generatorState.autoCycle,
                onCheckedChange = {
                    viewModel.setAutoCycle(it)
                    viewModel.saveSettings()
                }
            )
        }
        if (generatorState.autoCycle) {
            SliderWithLabel(
                label = "Auto cycle delay (seconds)",
                value = generatorState.autoCycleTime.toFloat(),
                onValueChange = { viewModel.setAutoCycleTime(it.roundToInt()) },
                onValueChangeFinished = { viewModel.saveSettings() },
                valueRange = Generator.AUTO_CYCLE_TIME_RANGE.toClosedFloatingPointRange(),
                steps = (Generator.AUTO_CYCLE_TIME_RANGE.endInclusive - Generator.AUTO_CYCLE_TIME_RANGE.start) / 5 - 1,
                valueDisplay = { String.format(Locale.US, "%02.1f", it) }
            )
        }
        if (showChannelASettings) {
            Dialog(
                onDismissRequest = { showChannelASettings = false }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    GeneratorParametersSettings(
                        viewModel = viewModel,
                        channel = 0,
                        generatorParameters = generatorState.channelAParameters,
                        title = "Channel A Settings"
                    )
                }
            }
        }

        if (showChannelBSettings) {
            Dialog(
                onDismissRequest = { showChannelBSettings = false }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    GeneratorParametersSettings(
                        viewModel = viewModel,
                        channel = 1,
                        generatorParameters = generatorState.channelBParameters,
                        title = "Channel B Settings"
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratorParametersSettings(
    viewModel: GeneratorViewModel,
    channel: Int,
    generatorParameters: GeneratorParameters,
    title: String,
    modifier: Modifier = Modifier
) {
    Column (modifier=modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)){
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        }
        SliderWithLabel(
            label = "Period (seconds)",
            value = generatorParameters.period.toFloat(),
            onValueChange = { viewModel.setPeriod(channel = channel, period = it.toDouble()) },
            onValueChangeFinished = { },
            valueRange = Generator.PERIOD_RANGE,
            steps = ((Generator.PERIOD_RANGE.endInclusive - Generator.PERIOD_RANGE.start) * 10.0 - 1).roundToInt(),
            valueDisplay = { String.format(Locale.US, "%02.1f", it) }
        )
        SliderWithLabel(
            label = "Time offset (seconds)",
            value = generatorParameters.timeOffset.toFloat(),
            onValueChange = { viewModel.setTimeOffset(channel = channel, seconds = it.toDouble()) },
            onValueChangeFinished = { },
            valueRange = 0.0f ..Generator.PERIOD_RANGE.endInclusive,
            steps = (Generator.PERIOD_RANGE.endInclusive * 10.0 - 1).roundToInt(),
            valueDisplay = { String.format(Locale.US, "%02.1f", it) }
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Wave shape")
            WaveTypePicker(generatorParameters.waveType, { viewModel.setWave(channel, it)})
        }
        SliderWithLabel(
            label = "Start power",
            value = generatorParameters.amplitudeLow.toFloat(),
            onValueChange = { viewModel.setMinPower(channel = channel, power = it.toDouble()) },
            onValueChangeFinished = { },
            valueRange = 0.0f .. 1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%.0f%%", it * 100.0) }
        )
        SliderWithLabel(
            label = "End power",
            value = generatorParameters.amplitudeHigh.toFloat(),
            onValueChange = { viewModel.setMaxPower(channel = channel, power = it.toDouble()) },
            onValueChangeFinished = { },
            valueRange = 0.0f .. 1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%.0f%%", it * 100.0) }
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Frequency shape")
            WaveTypePicker(generatorParameters.frequencyType, { viewModel.setFrequencyWave(channel, it)})
        }
        SliderWithLabel(
            label = "Start frequency",
            value = generatorParameters.frequencyLow.toFloat(),
            onValueChange = { viewModel.setMinFrequency(channel = channel, frequency = it.toDouble()) },
            onValueChangeFinished = { },
            valueRange = 0.0f .. 1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%.0f%%", it * 100.0) }
        )
        SliderWithLabel(
            label = "End frequency",
            value = generatorParameters.frequencyHigh.toFloat(),
            onValueChange = { viewModel.setMaxFrequency(channel = channel, frequency = it.toDouble()) },
            onValueChangeFinished = { },
            valueRange = 0.0f .. 1.0f,
            steps = 99,
            valueDisplay = { String.format(Locale.US, "%.0f%%", it * 100.0) }
        )
    }
}

@Composable
fun GeneratorParametersInfo(
    title: String,
    generatorParameters: GeneratorParameters,
    frequencyRange: ClosedFloatingPointRange<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column (modifier=modifier.fillMaxWidth()){
        Card (modifier=Modifier.fillMaxWidth().fillMaxHeight().clickable(onClick = onClick)){
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 0.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            GeneratorParametersInfoRow(
                category = "Shape",
                value = generatorParameters.waveType.description
            )
            GeneratorParametersInfoRow(
                category = "Period",
                value = String.format(Locale.US, "%.2f seconds", generatorParameters.period)
            )
            val ampLow = String.format(Locale.US, "%.1f", generatorParameters.amplitudeLow * 100.0)
            val ampHigh = String.format(Locale.US, "%.1f", generatorParameters.amplitudeHigh * 100.0)
            var ampText = if (generatorParameters.waveType == WaveType.Constant) "$ampHigh%" else "$ampLow% - $ampHigh%"
            GeneratorParametersInfoRow(
                category = "Power",
                value = ampText
            )
            GeneratorParametersInfoRow(
                category = "Freq shape",
                value = generatorParameters.frequencyType.description
            )
            val freqLow =
                frequencyRange.start + generatorParameters.frequencyLow * (frequencyRange.endInclusive - frequencyRange.start)
            val freqHigh =
                frequencyRange.start + generatorParameters.frequencyHigh * (frequencyRange.endInclusive - frequencyRange.start)
            val freqText =
                if (generatorParameters.frequencyType == WaveType.Constant)
                    String.format(Locale.US, "%.1fHz", freqHigh)
                else
                    String.format(Locale.US, "%.1fHz - %.1fHz", freqLow, freqHigh)
            GeneratorParametersInfoRow(
                category = "Freq",
                value = freqText
            )
            if(generatorParameters.timeOffset > 0.0) {
                GeneratorParametersInfoRow(
                    category = "Time offset",
                    value = String.format(Locale.US, "%.2f seconds", generatorParameters.timeOffset)
                )
            } else {
                GeneratorParametersInfoRow(" ", " ")
            }
        }
    }
}

@Composable
fun GeneratorParametersInfoRow(
    category: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = category,
            style = MaterialTheme.typography.bodySmall)
        Text(text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun WaveTypePicker(
    currentValue: WaveType,
    onValueChange: (WaveType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .wrapContentSize(Alignment.TopStart)) {

        OutlinedButton(onClick = { expanded = true }) {
            Text(text = currentValue.description)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val sortedWaveTypes = WaveType.entries.sortedBy { it.description }
            sortedWaveTypes.forEach { waveType ->
                DropdownMenuItem(text = {
                    Text(text = waveType.description)
                },
                onClick = {
                    onValueChange(waveType)
                    expanded = false
                })
            }
        }
    }
}

@Preview
@Composable
fun GeneratorPreview() {
    HowlTheme {
        val viewModel: GeneratorViewModel = viewModel()
        GeneratorPanel(
            viewModel = viewModel,
            frequencyRange = 10f..100f,
            modifier = Modifier.fillMaxHeight()
        )
    }
}