package com.example.howl

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.howl.ui.theme.HowlTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun getNextTimes(time: Double): List<Double> {
    val times: List<Double> = listOf(
        time,
        time + DGCoyote.PULSE_TIME,
        time + DGCoyote.PULSE_TIME * 2.0,
        time + DGCoyote.PULSE_TIME * 3.0
    )
    return times
}

class MainActivity : ComponentActivity() {
    private var mainTimerJob: Job? = null
    override fun onBackPressed() {
        //super.onBackPressed()
    }
    override fun onDestroy() {
        super.onDestroy()
        mainTimerJob?.cancel()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HowlTheme {
                val howlDatabase = HowlDatabase.getDatabase(this)
                DataRepository.initialise(db = howlDatabase)
                val mainTimerInterval = 100000000L
                val mainOptionsViewModel: MainOptionsViewModel = viewModel()
                val tabLayoutViewModel: TabLayoutViewModel = viewModel()
                val playerViewModel: PlayerViewModel = viewModel()
                val generatorViewModel: GeneratorViewModel = viewModel()
                val coyoteParametersViewModel: CoyoteParametersViewModel = viewModel()
                LaunchedEffect(true) {
                    DataRepository.loadSettings()
                }
                Generator.initialise()
                DGCoyote.initialize(context = this,
                    onConnectionStatusUpdate = { DataRepository.setCoyoteConnectionStatus(it) },
                    onBatteryLevelUpdate = { DataRepository.setCoyoteBatteryLevel(it) },
                    onPowerLevelUpdate = { channel:Int, power:Int ->
                        if(channel == 0)
                            DataRepository.setChannelAPower(power)
                        else if (channel == 1)
                            DataRepository.setChannelBPower(power)
                    } )

                LaunchedEffect(Unit) {
                    // Launch a coroutine that runs every 100ms
                    mainTimerJob = CoroutineScope(Dispatchers.Default).launch {
                        while (isActive) {
                            val startTime = System.nanoTime()
                            val playerState = DataRepository.funscriptPlayerState.value
                            val generatorState = DataRepository.generatorState.value
                            val mainOptionsState = DataRepository.mainOptionsState.value
                            val connected = DataRepository.coyoteConnectionStatus.value == ConnectionStatus.Connected
                            val swapChannels = DataRepository.mainOptionsState.value.swapChannels
                            if (generatorState.isPlaying) {
                                val currentTime = Generator.getCurrentTime()
                                val times = getNextTimes(currentTime)
                                val pulses: List<Pulse> = times.map {
                                    Generator.getPulseAtTime(time = it)
                                }
                                //Log.d("DGCoyote", "T=$currentTime $pulses")
                                if (connected == true and mainOptionsState.globalMute == false)
                                    DGCoyote.sendPulse(
                                        channelAStrength = mainOptionsState.channelAPower,
                                        channelBStrength = mainOptionsState.channelBPower,
                                        minFrequency = mainOptionsState.frequencyRange.start,
                                        maxFrequency = mainOptionsState.frequencyRange.endInclusive,
                                        swapChannels = swapChannels,
                                        pulseData = pulses
                                    )
                                if (generatorState.autoCycle && generatorState.autoCycleTime < currentTime)
                                    Generator.randomise()
                            }
                            else if (playerState.isPlaying) {
                                val currentPosition = Player.getCurrentPosition()
                                if (currentPosition > playerState.fileLength) {
                                    if (Player.shouldLoop) {
                                        Player.startPlayer(0.0)
                                        continue
                                    }
                                    else
                                        Player.stopPlayer()
                                }
                                else {
                                    val times = getNextTimes(currentPosition)
                                    val pulses: List<Pulse> = times.map {
                                        Player.getPulseAtTime(
                                            time = it,
                                        )
                                    }
                                    //Log.d("DGCoyote", "T=$currentPosition $pulses")
                                    if (connected == true and mainOptionsState.globalMute == false)
                                        DGCoyote.sendPulse(
                                            channelAStrength = mainOptionsState.channelAPower,
                                            channelBStrength = mainOptionsState.channelBPower,
                                            minFrequency = mainOptionsState.frequencyRange.start,
                                            maxFrequency = mainOptionsState.frequencyRange.endInclusive,
                                            swapChannels = swapChannels,
                                            pulseData = pulses
                                        )
                                    DataRepository.setFunscriptPlayerPosition(currentPosition)
                                }
                            }

                            val elapsedTime = System.nanoTime() - startTime
                            val toWait = max(mainTimerInterval - elapsedTime, 90000000L)
                            delay(toWait.toDuration(DurationUnit.NANOSECONDS))
                        }
                    }
                }

                val connectionStatus by DataRepository.coyoteConnectionStatus.collectAsStateWithLifecycle()
                val batteryPercent by DataRepository.coyoteBatteryLevel.collectAsStateWithLifecycle()
                val tabIndex by tabLayoutViewModel.tabIndex.collectAsStateWithLifecycle()

                Scaffold(
                    bottomBar = {
                        ConnectionStatusBar(connectionStatus, batteryPercent, { DGCoyote.connect(DataRepository.coyoteParametersState.value) })
                    }
                ) { innerPadding ->
                    Column (
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                    ){
                        MainOptionsPanel(
                            viewModel = mainOptionsViewModel
                        )
                        TabLayout (
                            tabIndex = tabIndex,
                            tabs = tabLayoutViewModel.tabs,
                            onTabChange = { tabLayoutViewModel.setTabIndex(it) },
                            playerViewModel = playerViewModel,
                            coyoteParametersViewModel = coyoteParametersViewModel,
                            generatorViewModel = generatorViewModel,
                            frequencyRange = mainOptionsViewModel.mainOptionsState.collectAsStateWithLifecycle().value.frequencyRange
                        )
                    }
                }
            }
        }
    }
}

