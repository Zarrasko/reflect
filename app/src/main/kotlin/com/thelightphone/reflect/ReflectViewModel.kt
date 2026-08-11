package com.thelightphone.reflect

import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class ReflectUiState(
    val currentPrompt: ReflectPrompt,
    val isDailyPrompt: Boolean = true,
)

class ReflectViewModel : LightViewModel<Unit>() {
    private var currentDate: LocalDate = todaysDate()

    // Fixed, randomly-ordered rotation of today's prompts. Shuffle just
    // advances through it, so all 5 are seen once before any repeat.
    private var dailyOrder: List<ReflectPrompt> = PromptList.dailyPromptSet(currentDate)
    private var dailyIndex: Int = 0

    private val _uiState = MutableStateFlow(
        ReflectUiState(currentPrompt = dailyOrder[dailyIndex], isDailyPrompt = true),
    )
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        refreshForNewDayIfNeeded()
    }

    fun shufflePrompt() {
        dailyIndex = (dailyIndex + 1) % dailyOrder.size
        _uiState.value = ReflectUiState(
            currentPrompt = dailyOrder[dailyIndex],
            isDailyPrompt = dailyIndex == 0,
        )
    }

    private fun refreshForNewDayIfNeeded() {
        val today = todaysDate()
        if (today == currentDate) return
        currentDate = today
        dailyOrder = PromptList.dailyPromptSet(today)
        dailyIndex = 0
        _uiState.value = ReflectUiState(currentPrompt = dailyOrder[dailyIndex], isDailyPrompt = true)
    }

    private fun todaysDate(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
