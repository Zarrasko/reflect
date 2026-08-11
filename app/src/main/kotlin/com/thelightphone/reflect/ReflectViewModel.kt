package com.thelightphone.reflect

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

sealed class ReflectScreenMode {
    data object Card : ReflectScreenMode()
    data object TodayList : ReflectScreenMode()
    data object Settings : ReflectScreenMode()
}

data class ReflectUiState(
    val mode: ReflectScreenMode = ReflectScreenMode.Card,
    val currentPrompt: ReflectPrompt,
    val isDailyPrompt: Boolean = true,
    val dailyOrder: List<ReflectPrompt>,
    val showList: Boolean = false,
    val selectedCategories: Set<PromptCategory> = PromptCategory.entries.toSet(),
)

class ReflectViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private var currentDate: LocalDate = todaysDate()

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadPreferences()
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        refreshForNewDayIfNeeded()
    }

    fun shufflePrompt() {
        _uiState.update { state ->
            val order = state.dailyOrder
            val currentIndex = order.indexOf(state.currentPrompt).coerceAtLeast(0)
            val nextIndex = (currentIndex + 1) % order.size
            state.copy(currentPrompt = order[nextIndex], isDailyPrompt = nextIndex == 0)
        }
    }

    fun openTodayList() {
        _uiState.update { it.copy(mode = ReflectScreenMode.TodayList) }
    }

    fun openSettings() {
        _uiState.update { it.copy(mode = ReflectScreenMode.Settings) }
    }

    fun closeSubScreen() {
        _uiState.update { it.copy(mode = ReflectScreenMode.Card) }
    }

    fun selectPromptFromList(prompt: ReflectPrompt) {
        _uiState.update { state ->
            val index = state.dailyOrder.indexOf(prompt).coerceAtLeast(0)
            state.copy(mode = ReflectScreenMode.Card, currentPrompt = prompt, isDailyPrompt = index == 0)
        }
    }

    fun setShowList(enabled: Boolean) {
        _uiState.update { it.copy(showList = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs -> prefs[ReflectPreferences.SHOW_LIST] = enabled }
        }
    }

    fun toggleCategory(category: PromptCategory) {
        val current = _uiState.value.selectedCategories
        val updated = if (category in current) current - category else current + category
        if (updated.isEmpty()) return

        applyCategories(updated)
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[ReflectPreferences.SELECTED_CATEGORIES] = updated.map { it.name }.toSet()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (_uiState.value.mode != ReflectScreenMode.Card) {
            closeSubScreen()
            return true
        }
        return false
    }

    private fun applyCategories(categories: Set<PromptCategory>) {
        val order = PromptList.dailyPromptSet(currentDate, categories)
        _uiState.update {
            it.copy(
                selectedCategories = categories,
                dailyOrder = order,
                currentPrompt = order.first(),
                isDailyPrompt = true,
            )
        }
    }

    private suspend fun loadPreferences() {
        val prefs = dataStore.data.first()
        val showList = prefs[ReflectPreferences.SHOW_LIST] ?: false
        val storedNames = prefs[ReflectPreferences.SELECTED_CATEGORIES]
        val categories = storedNames
            ?.mapNotNull { name -> PromptCategory.entries.find { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: PromptCategory.entries.toSet()

        _uiState.update { it.copy(showList = showList) }
        if (categories != _uiState.value.selectedCategories) {
            applyCategories(categories)
        }
    }

    private fun refreshForNewDayIfNeeded() {
        val today = todaysDate()
        if (today == currentDate) return
        currentDate = today
        applyCategories(_uiState.value.selectedCategories)
    }

    private fun initialState(): ReflectUiState {
        val categories = PromptCategory.entries.toSet()
        val order = PromptList.dailyPromptSet(currentDate, categories)
        return ReflectUiState(
            currentPrompt = order.first(),
            dailyOrder = order,
            selectedCategories = categories,
        )
    }

    private fun todaysDate(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
