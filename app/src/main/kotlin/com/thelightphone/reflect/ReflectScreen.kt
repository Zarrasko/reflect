package com.thelightphone.reflect

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.rememberPermissionRequestLauncher
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightShakeDetector
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.defaultKeyboardOptions
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

@InitialScreen
class ReflectScreen(private val sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ReflectViewModel>(sealedActivity) {

    override val viewModelClass: Class<ReflectViewModel>
        get() = ReflectViewModel::class.java

    override fun createViewModel(): ReflectViewModel = ReflectViewModel(
        dataStore = lightContext.dataStore,
        entryDao = lightContext.buildDatabase(ReflectDatabase::class.java, "reflect.db").entryDao(),
        filesDir = lightContext.filesDir,
        audio = DefaultLightAudio(sealedActivity),
    )

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val micPermissionLauncher = rememberPermissionRequestLauncher(Manifest.permission.RECORD_AUDIO)

        LightShakeDetector(onShake = viewModel::shufflePrompt)

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                when (state.mode) {
                    ReflectScreenMode.Card -> PromptCardContent(
                        prompt = state.currentPrompt,
                        isDailyPrompt = state.isDailyPrompt,
                        isStarred = state.currentPrompt.id in state.starredPromptIds,
                        showListButton = state.showList,
                        pastEntryCount = state.entriesForCurrentPrompt.size,
                        onShuffle = viewModel::shufflePrompt,
                        onOpenList = viewModel::openTodayList,
                        onOpenSettings = viewModel::openSettings,
                        onOpenHistory = viewModel::openGlobalHistory,
                        onToggleStar = { viewModel.toggleStar(state.currentPrompt.id) },
                        onNewEntry = viewModel::openNewEntry,
                        onOpenPastEntries = viewModel::openPromptHistory,
                    )

                    ReflectScreenMode.TodayList -> TodayListContent(
                        prompts = state.dailyOrder,
                        selectedId = state.currentPrompt.id,
                        onSelect = viewModel::selectPromptFromList,
                        onBack = viewModel::closeSubScreen,
                    )

                    ReflectScreenMode.Settings -> SettingsContent(
                        showList = state.showList,
                        selectedCategories = state.selectedCategories,
                        onToggleShowList = viewModel::setShowList,
                        onToggleCategory = viewModel::toggleCategory,
                        onBack = viewModel::closeSubScreen,
                    )

                    ReflectScreenMode.GlobalHistory -> HistoryListContent(
                        title = "History",
                        entries = state.globalHistoryEntries,
                        onSelect = { viewModel.openEntry(it.id) },
                        onBack = viewModel::closeSubScreen,
                    )

                    ReflectScreenMode.PromptHistory -> HistoryListContent(
                        title = "Past Entries",
                        entries = state.promptHistoryEntries,
                        onSelect = { viewModel.openEntry(it.id) },
                        onBack = viewModel::closeSubScreen,
                    )

                    ReflectScreenMode.Editor -> state.editorState?.let { editor ->
                        EntryEditorContent(
                            editor = editor,
                            onEditText = viewModel::openEditText,
                            onToggleRecording = {
                                if (editor.isRecording) viewModel.stopRecording() else viewModel.startRecording()
                            },
                            onRequestMicPermission = { micPermissionLauncher?.launch() },
                            onTogglePlayback = viewModel::togglePlayback,
                            onRemoveAudio = viewModel::removeAudio,
                            onSave = viewModel::saveEntry,
                            onCancel = viewModel::cancelEntry,
                            onDelete = viewModel::deleteEntry,
                        )
                    }

                    ReflectScreenMode.EditText -> state.editorState?.let { editor ->
                        key(editor.entryId ?: -1L) {
                            val textFieldState = rememberTextFieldState(editor.text)
                            val keyboardOptionsFlow = remember { MutableStateFlow(defaultKeyboardOptions()) }
                            LightTextInputEditor(
                                title = "Write",
                                state = textFieldState,
                                keyboardOptionsFlow = keyboardOptionsFlow,
                                onSubmit = { viewModel.updateEditorText(it.toString()) },
                                onBack = { viewModel.updateEditorText(textFieldState.text.toString()) },
                                submitLabel = "Done",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptCardContent(
    prompt: ReflectPrompt,
    isDailyPrompt: Boolean,
    isStarred: Boolean,
    showListButton: Boolean,
    pastEntryCount: Int,
    onShuffle: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleStar: () -> Unit,
    onNewEntry: () -> Unit,
    onOpenPastEntries: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.ALARM,
                onClick = onOpenHistory,
                contentDescription = "History",
            ),
            center = LightTopBarCenter.Text("Reflect"),
            rightButton = LightBarButton.LightIcon(
                icon = LightIcons.SETTINGS,
                onClick = onOpenSettings,
                contentDescription = "Settings",
            ),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = LightThemeTokens.colors.content)
                        .padding(1.5f.gridUnitsAsDp()),
                ) {
                    LightText(
                        text = if (isDailyPrompt) "TODAY'S PROMPT" else "PROMPT",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )
                    LightText(
                        text = prompt.text,
                        variant = LightTextVariant.Heading,
                    )
                }
                if (pastEntryCount > 0) {
                    LightText(
                        text = if (pastEntryCount == 1) "1 past entry" else "$pastEntryCount past entries",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier
                            .padding(top = 1f.gridUnitsAsDp())
                            .lightClickable(onClick = onOpenPastEntries),
                    )
                }
            }
        }

        LightBottomBar(
            items = buildList {
                add(
                    LightBarButton.LightIcon(
                        icon = LightIcons.SHUFFLE,
                        onClick = onShuffle,
                        contentDescription = "New prompt",
                    ),
                )
                if (showListButton) {
                    add(
                        LightBarButton.LightIcon(
                            icon = LightIcons.LIST,
                            onClick = onOpenList,
                            contentDescription = "Today's prompts",
                        ),
                    )
                }
                add(
                    LightBarButton.LightIcon(
                        icon = if (isStarred) LightIcons.STAR else LightIcons.STAR_OUTLINE,
                        onClick = onToggleStar,
                        contentDescription = "Star prompt",
                    ),
                )
                add(
                    LightBarButton.LightIcon(
                        icon = LightIcons.PENCIL,
                        onClick = onNewEntry,
                        contentDescription = "New entry",
                    ),
                )
            },
        )
    }
}

@Composable
private fun TodayListContent(
    prompts: List<ReflectPrompt>,
    selectedId: Int,
    onSelect: (ReflectPrompt) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text("Today's Prompts"),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )

        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 1f.gridUnitsAsDp()),
        ) {
            prompts.forEach { prompt ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .lightClickable(onClick = { onSelect(prompt) })
                        .padding(bottom = 1f.gridUnitsAsDp()),
                ) {
                    LightText(
                        text = prompt.text,
                        variant = LightTextVariant.Copy,
                        underline = prompt.id == selectedId,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    showList: Boolean,
    selectedCategories: Set<PromptCategory>,
    onToggleShowList: (Boolean) -> Unit,
    onToggleCategory: (PromptCategory) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text("Settings"),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )

        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .lightClickable(onClick = { onToggleShowList(!showList) })
                    .padding(vertical = 1f.gridUnitsAsDp()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LightText(
                    text = "Show today's list",
                    variant = LightTextVariant.Copy,
                    modifier = Modifier.weight(1f),
                )
                LightIcon(
                    icon = if (showList) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF,
                    contentDescription = null,
                )
            }

            LightText(
                text = "CATEGORIES",
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 1f.gridUnitsAsDp(), bottom = 0.5f.gridUnitsAsDp()),
            )

            PromptCategory.entries.forEach { category ->
                val selected = category in selectedCategories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .lightClickable(onClick = { onToggleCategory(category) })
                        .padding(vertical = 0.75f.gridUnitsAsDp()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LightText(
                        text = category.label,
                        variant = LightTextVariant.Copy,
                        modifier = Modifier.weight(1f),
                    )
                    LightIcon(
                        icon = if (selected) LightIcons.SELECT_ON else LightIcons.SELECT_OFF,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryListContent(
    title: String,
    entries: List<JournalEntryUi>,
    onSelect: (JournalEntryUi) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(
                icon = LightIcons.BACK,
                onClick = onBack,
                contentDescription = "Back",
            ),
            center = LightTopBarCenter.Text(title),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LightText(
                    text = "No entries yet",
                    variant = LightTextVariant.Copy,
                    lighten = true,
                )
            }
        } else {
            LightScrollView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 1f.gridUnitsAsDp()),
            ) {
                entries.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .lightClickable(onClick = { onSelect(entry) })
                            .padding(bottom = 1.5f.gridUnitsAsDp()),
                    ) {
                        LightText(
                            text = entry.text.ifBlank { entry.promptText },
                            variant = LightTextVariant.Heading,
                            maxLines = 2,
                        )
                        Row(
                            modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LightIcon(
                                icon = if (entry.audioFileName != null) LightIcons.VOICE_MEMO else LightIcons.PENCIL,
                                contentDescription = null,
                                size = 1.25f,
                                modifier = Modifier.padding(end = 0.5f.gridUnitsAsDp()),
                            )
                            LightText(
                                text = formatEntryDate(entry.createdAtEpochMillis),
                                variant = LightTextVariant.Copy,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryEditorContent(
    editor: EntryEditorState,
    onEditText: () -> Unit,
    onToggleRecording: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRemoveAudio: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            center = LightTopBarCenter.Text(if (editor.entryId == null) "New Entry" else "Edit Entry"),
            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
        )

        LightScrollView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            LightText(
                text = editor.promptText,
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = LightThemeTokens.colors.content)
                    .lightClickable(onClick = onEditText)
                    .padding(1f.gridUnitsAsDp()),
            ) {
                LightText(
                    text = editor.text.ifBlank { "Tap to write" },
                    variant = LightTextVariant.Copy,
                    lighten = editor.text.isBlank(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.5f.gridUnitsAsDp()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    editor.isRecording -> {
                        LightIcon(
                            icon = LightIcons.STOP,
                            contentDescription = "Stop recording",
                            modifier = Modifier.lightClickable(onClick = onToggleRecording),
                        )
                        LightText(
                            text = "Recording  ${formatDuration(editor.recordingElapsedMs)}",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(start = 1f.gridUnitsAsDp()),
                        )
                    }

                    editor.hasAudio -> {
                        LightIcon(
                            icon = if (editor.isPlaying) LightIcons.PAUSE else LightIcons.PLAY,
                            contentDescription = "Play voice note",
                            modifier = Modifier.lightClickable(onClick = onTogglePlayback),
                        )
                        LightText(
                            text = formatDuration(
                                if (editor.isPlaying) editor.playbackPositionMs else editor.audioDurationMs,
                            ),
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(start = 1f.gridUnitsAsDp()).weight(1f),
                        )
                        LightIcon(
                            icon = LightIcons.TRASH,
                            contentDescription = "Remove voice note",
                            modifier = Modifier.lightClickable(onClick = onRemoveAudio),
                        )
                    }

                    !editor.microphonePermissionGranted -> {
                        LightIcon(
                            icon = LightIcons.MICROPHONE,
                            contentDescription = "Allow microphone",
                            modifier = Modifier.lightClickable(onClick = onRequestMicPermission),
                        )
                        LightText(
                            text = "Allow microphone to record",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                            modifier = Modifier.padding(start = 1f.gridUnitsAsDp()),
                        )
                    }

                    else -> {
                        LightIcon(
                            icon = LightIcons.MICROPHONE,
                            contentDescription = "Record voice note",
                            modifier = Modifier.lightClickable(onClick = onToggleRecording),
                        )
                        LightText(
                            text = "Record a voice note",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                            modifier = Modifier.padding(start = 1f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }

        LightBottomBar(
            items = buildList {
                add(LightBarButton.Text(text = "Cancel", onClick = onCancel))
                if (editor.entryId != null) {
                    add(LightBarButton.LightIcon(icon = LightIcons.TRASH, onClick = onDelete, contentDescription = "Delete"))
                }
                add(LightBarButton.Text(text = "Save", onClick = onSave))
            },
        )
    }
}

private val entryDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

private fun formatEntryDate(epochMillis: Long): String = entryDateFormat.format(epochMillis)

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
