package com.example.howl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.howl.ui.theme.HowlTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.Float
import kotlin.ranges.ClosedFloatingPointRange

class TabLayoutViewModel() : ViewModel() {
    private val _tabIndex = MutableStateFlow(0)
    val tabIndex: StateFlow<Int> = _tabIndex.asStateFlow()
    val tabs = listOf("Player", "Generator", "Settings")

    fun setTabIndex(index: Int) {
        _tabIndex.update { index }
    }
}

@Composable
fun TabLayout(
    tabIndex: Int,
    tabs: List<String>,
    onTabChange: (index: Int) -> Unit,
    playerViewModel: PlayerViewModel,
    coyoteParametersViewModel: CoyoteParametersViewModel,
    generatorViewModel: GeneratorViewModel,
    frequencyRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(text = { Text(title) },
                    selected = tabIndex == index,
                    onClick = { onTabChange(index) },
                    icon = {
                        when (index) {
                            0 -> Icon(painter = painterResource(R.drawable.player), contentDescription = null)
                            1 -> Icon(painter = painterResource(R.drawable.wave), contentDescription = null)
                            2 -> Icon(painter = painterResource(R.drawable.settings), contentDescription = null)
                        }
                    }
                )
            }
        }

        when (tabIndex) {
            0 -> PlayerPanel(viewModel = playerViewModel)
            1 -> GeneratorPanel(
                viewModel = generatorViewModel,
                frequencyRange = frequencyRange
            )
            2 -> CoyoteParametersPanel(viewModel = coyoteParametersViewModel)
        }
    }
}

@Preview
@Composable
fun TabLayoutPreview() {
    HowlTheme {
        val viewModel: TabLayoutViewModel = viewModel()
        val playerViewModel: PlayerViewModel = viewModel()
        val coyoteParametersViewModel: CoyoteParametersViewModel = viewModel()
        val generatorViewModel: GeneratorViewModel = viewModel()
        TabLayout (
            tabIndex = 0,
            tabs = viewModel.tabs,
            onTabChange = {},
            playerViewModel = playerViewModel,
            coyoteParametersViewModel = coyoteParametersViewModel,
            generatorViewModel = generatorViewModel,
            frequencyRange = 0f..100f,
            modifier = Modifier.fillMaxHeight()
        )
    }
}