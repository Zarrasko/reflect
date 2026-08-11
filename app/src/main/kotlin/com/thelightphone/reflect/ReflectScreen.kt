package com.thelightphone.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightShakeDetector
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

@InitialScreen
class ReflectScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ReflectViewModel>(sealedActivity) {

    override val viewModelClass: Class<ReflectViewModel>
        get() = ReflectViewModel::class.java

    override fun createViewModel(): ReflectViewModel = ReflectViewModel(lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

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
                        showListButton = state.showList,
                        onShuffle = viewModel::shufflePrompt,
                        onOpenList = viewModel::openTodayList,
                        onOpenSettings = viewModel::openSettings,
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
                }
            }
        }
    }
}

@Composable
private fun PromptCardContent(
    prompt: ReflectPrompt,
    isDailyPrompt: Boolean,
    showListButton: Boolean,
    onShuffle: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
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
