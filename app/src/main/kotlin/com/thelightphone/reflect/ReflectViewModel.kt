package com.thelightphone.reflect

import android.Manifest
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.audio.LightAudioException
import com.thelightphone.sdk.audio.LightAudioPlayer
import com.thelightphone.sdk.audio.LightAudioRecorder
import com.thelightphone.sdk.checkPermission
import com.thelightphone.sdk.shared.LightServiceMethod
import com.thelightphone.sdk.shared.asKotlinResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.util.UUID
import kotlin.time.Clock

sealed class ReflectScreenMode {
    data object Card : ReflectScreenMode()
    data object TodayList : ReflectScreenMode()
    data object Settings : ReflectScreenMode()
    data object GlobalHistory : ReflectScreenMode()
    data object PromptHistory : ReflectScreenMode()
    data object Editor : ReflectScreenMode()
    data object EditText : ReflectScreenMode()
}

data class ReflectUiState(
    val mode: ReflectScreenMode = ReflectScreenMode.Card,
    val currentPrompt: ReflectPrompt,
    val isDailyPrompt: Boolean = true,
    val dailyOrder: List<ReflectPrompt>,
    val showList: Boolean = false,
    val selectedCategories: Set<PromptCategory> = PromptCategory.entries.toSet(),
    val starredPromptIds: Set<Int> = emptySet(),
    val entriesForCurrentPrompt: List<JournalEntryUi> = emptyList(),
    val promptHistoryEntries: List<JournalEntryUi> = emptyList(),
    val promptHistoryTitle: String = "",
    val globalHistoryEntries: List<JournalEntryUi> = emptyList(),
    val editorState: EntryEditorState? = null,
)

class ReflectViewModel(
    private val dataStore: DataStore<Preferences>,
    private val entryDao: JournalEntryDao,
    private val filesDir: File,
    private val audio: LightAudio,
) : LightViewModel<Unit>() {
    private var currentDate: LocalDate = todaysDate()
    private var shownNonStarredIds: MutableSet<Int> = mutableSetOf()

    private val recorder: LightAudioRecorder = audio.newRecorder()
    private val player: LightAudioPlayer = audio.newPlayer()
    private var elapsedJob: Job? = null
    private var playbackJob: Job? = null

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadPreferences()
            loadEntriesForCurrentPrompt()
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        refreshForNewDayIfNeeded()
        if (_uiState.value.mode == ReflectScreenMode.Editor) refreshMicPermission()
    }

    override fun onCleared() {
        elapsedJob?.cancel()
        playbackJob?.cancel()
        recorder.release()
        player.release()
        super.onCleared()
    }

    // region prompt navigation

    fun shufflePrompt() {
        val state = _uiState.value
        val order = state.dailyOrder
        val starred = state.starredPromptIds
        val nonStarredIds = order.map { it.id }.filterNot { it in starred }.toSet()

        if (nonStarredIds.isNotEmpty() && shownNonStarredIds.containsAll(nonStarredIds)) {
            shownNonStarredIds.clear()
        }

        var candidates = order.filter { it.id in starred || it.id !in shownNonStarredIds }
        if (candidates.size > 1) {
            candidates = candidates.filter { it.id != state.currentPrompt.id }
        }
        val next = candidates.randomOrNull() ?: order.first()
        if (next.id !in starred) shownNonStarredIds.add(next.id)

        selectPrompt(next, isDaily = next.id == order.first().id)
    }

    fun selectPromptFromList(prompt: ReflectPrompt) {
        selectPrompt(prompt, isDaily = prompt.id == _uiState.value.dailyOrder.first().id)
        _uiState.update { it.copy(mode = ReflectScreenMode.Card) }
    }

    private fun selectPrompt(prompt: ReflectPrompt, isDaily: Boolean) {
        _uiState.update { it.copy(currentPrompt = prompt, isDailyPrompt = isDaily) }
        loadEntriesForCurrentPrompt()
    }

    fun toggleStar(promptId: Int) {
        val current = _uiState.value.starredPromptIds
        val updated = if (promptId in current) current - promptId else current + promptId
        _uiState.update { it.copy(starredPromptIds = updated) }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[ReflectPreferences.STARRED_PROMPT_IDS] = updated.map { it.toString() }.toSet()
            }
        }
    }

    // endregion

    // region navigation

    fun openTodayList() = _uiState.update { it.copy(mode = ReflectScreenMode.TodayList) }
    fun openSettings() = _uiState.update { it.copy(mode = ReflectScreenMode.Settings) }

    fun openGlobalHistory() {
        _uiState.update { it.copy(mode = ReflectScreenMode.GlobalHistory) }
        viewModelScope.launch(Dispatchers.IO) {
            val entries = entryDao.getAllEntries().map { it.toUi() }
            _uiState.update { it.copy(globalHistoryEntries = entries) }
        }
    }

    fun openPromptHistory() {
        val prompt = _uiState.value.currentPrompt
        _uiState.update {
            it.copy(mode = ReflectScreenMode.PromptHistory, promptHistoryTitle = prompt.text)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val entries = entryDao.getEntriesForPrompt(prompt.id).map { it.toUi() }
            _uiState.update { it.copy(promptHistoryEntries = entries) }
        }
    }

    fun closeSubScreen() {
        stopPlaybackAndRecording()
        _uiState.update { it.copy(mode = ReflectScreenMode.Card) }
        loadEntriesForCurrentPrompt()
    }

    override fun onBackPressed(): Boolean {
        val mode = _uiState.value.mode
        if (mode == ReflectScreenMode.EditText) {
            _uiState.update { it.copy(mode = ReflectScreenMode.Editor) }
            return true
        }
        if (mode != ReflectScreenMode.Card) {
            closeSubScreen()
            return true
        }
        return false
    }

    // endregion

    // region entry editor

    fun openNewEntry() {
        val prompt = _uiState.value.currentPrompt
        _uiState.update {
            it.copy(
                mode = ReflectScreenMode.Editor,
                editorState = EntryEditorState(
                    entryId = null,
                    promptId = prompt.id,
                    promptText = prompt.text,
                    text = "",
                    originalAudioFileName = null,
                    recordedAudioFileName = null,
                ),
            )
        }
        refreshMicPermission()
    }

    fun openEntry(entryId: Long) {
        _uiState.update { it.copy(mode = ReflectScreenMode.Editor) }
        viewModelScope.launch(Dispatchers.IO) {
            val entry = entryDao.getEntry(entryId) ?: return@launch
            _uiState.update {
                it.copy(
                    editorState = EntryEditorState(
                        entryId = entry.id,
                        promptId = entry.promptId,
                        promptText = entry.promptText,
                        text = entry.text,
                        originalAudioFileName = entry.audioFileName,
                        recordedAudioFileName = null,
                        audioDurationMs = entry.audioDurationMs,
                    ),
                )
            }
        }
        refreshMicPermission()
    }

    fun openEditText() = _uiState.update { it.copy(mode = ReflectScreenMode.EditText) }

    fun updateEditorText(text: String) {
        _uiState.update { state ->
            state.copy(editorState = state.editorState?.copy(text = text), mode = ReflectScreenMode.Editor)
        }
    }

    private fun refreshMicPermission() {
        viewModelScope.launch {
            val granted = checkPermission(Manifest.permission.RECORD_AUDIO).asKotlinResult
                .map { it.permissionResult == LightServiceMethod.GetPermission.Result.Granted }
                .getOrDefault(false)
            _uiState.update { state ->
                state.copy(editorState = state.editorState?.copy(microphonePermissionGranted = granted))
            }
        }
    }

    fun startRecording() {
        val editor = _uiState.value.editorState ?: return
        if (editor.isRecording) return
        player.stop()
        editor.recordedAudioFileName?.let { deleteAudioFile(it) }

        val fileName = "${UUID.randomUUID()}.m4a"
        val file = voiceNoteFile(fileName)
        try {
            recorder.start(file)
        } catch (_: LightAudioException) {
            return
        }
        _uiState.update { state ->
            state.copy(
                editorState = state.editorState?.copy(
                    isRecording = true,
                    recordingElapsedMs = 0L,
                    recordedAudioFileName = fileName,
                    audioRemoved = false,
                ),
            )
        }
        elapsedJob?.cancel()
        val startedAt = SystemClock.elapsedRealtime()
        elapsedJob = viewModelScope.launch {
            while (isActive && _uiState.value.editorState?.isRecording == true) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                _uiState.update { state ->
                    state.copy(editorState = state.editorState?.copy(recordingElapsedMs = elapsed))
                }
                delay(RECORDING_TICK_MS)
            }
        }
    }

    fun stopRecording() {
        elapsedJob?.cancel()
        elapsedJob = null
        val durationMs = recorder.stop()
        _uiState.update { state ->
            val editor = state.editorState ?: return@update state
            if (durationMs <= 0L) {
                editor.recordedAudioFileName?.let { deleteAudioFile(it) }
                state.copy(editorState = editor.copy(isRecording = false, recordedAudioFileName = null))
            } else {
                state.copy(editorState = editor.copy(isRecording = false, audioDurationMs = durationMs))
            }
        }
    }

    fun removeAudio() {
        stopPlaybackAndRecording()
        _uiState.update { state ->
            val editor = state.editorState ?: return@update state
            editor.recordedAudioFileName?.let { deleteAudioFile(it) }
            state.copy(editorState = editor.copy(recordedAudioFileName = null, audioRemoved = true, audioDurationMs = 0L))
        }
    }

    fun togglePlayback() {
        val editor = _uiState.value.editorState ?: return
        val fileName = editor.activeAudioFileName ?: return
        if (player.isPlaying.value) {
            player.pause()
            return
        }
        player.setSource(voiceNoteFile(fileName))
        player.play()
        observePlayback()
    }

    private fun observePlayback() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            launch {
                player.isPlaying.collect { playing ->
                    _uiState.update { state -> state.copy(editorState = state.editorState?.copy(isPlaying = playing)) }
                }
            }
            launch {
                player.positionMs.collect { pos ->
                    _uiState.update { state -> state.copy(editorState = state.editorState?.copy(playbackPositionMs = pos)) }
                }
            }
        }
    }

    private fun stopPlaybackAndRecording() {
        if (_uiState.value.editorState?.isRecording == true) stopRecording()
        player.stop()
        playbackJob?.cancel()
    }

    fun saveEntry() {
        val editor = _uiState.value.editorState ?: return
        stopPlaybackAndRecording()
        val now = System.currentTimeMillis()
        val finalAudioFileName = editor.activeAudioFileName
        val finalAudioDuration = if (finalAudioFileName != null) editor.audioDurationMs else 0L
        val staleFileToDelete = editor.originalAudioFileName
            ?.takeIf { it != finalAudioFileName }
        val promptId = editor.promptId

        viewModelScope.launch(Dispatchers.IO) {
            if (editor.entryId == null) {
                entryDao.insert(
                    JournalEntryEntity(
                        promptId = editor.promptId,
                        promptText = editor.promptText,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                        text = editor.text,
                        audioFileName = finalAudioFileName,
                        audioDurationMs = finalAudioDuration,
                    ),
                )
            } else {
                val existing = entryDao.getEntry(editor.entryId)
                if (existing != null) {
                    entryDao.update(
                        existing.copy(
                            text = editor.text,
                            updatedAtEpochMillis = now,
                            audioFileName = finalAudioFileName,
                            audioDurationMs = finalAudioDuration,
                        ),
                    )
                }
            }
            staleFileToDelete?.let { deleteAudioFile(it) }
            finishEditing(promptId)
        }
    }

    fun cancelEntry() {
        val editor = _uiState.value.editorState
        stopPlaybackAndRecording()
        editor?.recordedAudioFileName?.let { deleteAudioFile(it) }
        closeSubScreen()
    }

    fun deleteEntry() {
        val editor = _uiState.value.editorState ?: return
        val entryId = editor.entryId ?: return
        val promptId = editor.promptId
        stopPlaybackAndRecording()
        editor.recordedAudioFileName?.let { deleteAudioFile(it) }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = entryDao.getEntry(entryId)
            if (existing != null) {
                entryDao.delete(existing)
                existing.audioFileName?.let { deleteAudioFile(it) }
            }
            finishEditing(promptId)
        }
    }

    /** Runs on the IO dispatcher, after a save/delete write has completed. */
    private suspend fun finishEditing(promptId: Int) {
        val entries = entryDao.getEntriesForPrompt(promptId).map { it.toUi() }
        _uiState.update { state ->
            state.copy(
                mode = ReflectScreenMode.Card,
                entriesForCurrentPrompt = if (state.currentPrompt.id == promptId) entries else state.entriesForCurrentPrompt,
            )
        }
    }

    private fun voiceNoteFile(fileName: String): File {
        val dir = File(filesDir, "voice_notes").apply { mkdirs() }
        return File(dir, fileName)
    }

    private fun deleteAudioFile(fileName: String) {
        runCatching { voiceNoteFile(fileName).delete() }
    }

    // endregion

    // region settings

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

    // endregion

    private fun loadEntriesForCurrentPrompt() {
        val promptId = _uiState.value.currentPrompt.id
        viewModelScope.launch(Dispatchers.IO) {
            val entries = entryDao.getEntriesForPrompt(promptId).map { it.toUi() }
            _uiState.update {
                if (it.currentPrompt.id == promptId) it.copy(entriesForCurrentPrompt = entries) else it
            }
        }
    }

    private fun applyCategories(categories: Set<PromptCategory>) {
        val starred = _uiState.value.starredPromptIds
        val order = PromptList.dailyPromptSet(currentDate, categories, starred)
        shownNonStarredIds = mutableSetOf<Int>().apply {
            val first = order.first()
            if (first.id !in starred) add(first.id)
        }
        _uiState.update {
            it.copy(
                selectedCategories = categories,
                dailyOrder = order,
                currentPrompt = order.first(),
                isDailyPrompt = true,
            )
        }
        loadEntriesForCurrentPrompt()
    }

    private suspend fun loadPreferences() {
        val prefs = dataStore.data.first()
        val showList = prefs[ReflectPreferences.SHOW_LIST] ?: false
        val starredNames = prefs[ReflectPreferences.STARRED_PROMPT_IDS]
        val starred = starredNames?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val storedNames = prefs[ReflectPreferences.SELECTED_CATEGORIES]
        val categories = storedNames
            ?.mapNotNull { name -> PromptCategory.entries.find { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: PromptCategory.entries.toSet()

        val categoriesChanged = categories != _uiState.value.selectedCategories
        val starredChanged = starred != _uiState.value.starredPromptIds

        _uiState.update { it.copy(showList = showList, starredPromptIds = starred) }
        if (categoriesChanged || starredChanged) {
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

    companion object {
        private const val RECORDING_TICK_MS = 100L
    }
}
