package com.thelightphone.reflect

data class JournalEntryUi(
    val id: Long,
    val promptId: Int,
    val promptText: String,
    val createdAtEpochMillis: Long,
    val text: String,
    val audioFileName: String?,
    val audioDurationMs: Long,
)

internal fun JournalEntryEntity.toUi(): JournalEntryUi = JournalEntryUi(
    id = id,
    promptId = promptId,
    promptText = promptText,
    createdAtEpochMillis = createdAtEpochMillis,
    text = text,
    audioFileName = audioFileName,
    audioDurationMs = audioDurationMs,
)

data class EntryEditorState(
    val entryId: Long?,
    val promptId: Int,
    val promptText: String,
    val text: String,
    val originalAudioFileName: String?,
    val recordedAudioFileName: String?,
    val audioRemoved: Boolean = false,
    val audioDurationMs: Long = 0L,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val microphonePermissionGranted: Boolean = false,
) {
    val hasAudio: Boolean get() = !audioRemoved && (recordedAudioFileName != null || originalAudioFileName != null)
    val activeAudioFileName: String? get() = if (audioRemoved) null else (recordedAudioFileName ?: originalAudioFileName)
}
