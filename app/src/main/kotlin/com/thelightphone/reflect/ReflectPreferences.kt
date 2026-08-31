package com.thelightphone.reflect

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object ReflectPreferences {
    val SHOW_LIST = booleanPreferencesKey("show_list")
    val SELECTED_CATEGORIES = stringSetPreferencesKey("selected_categories")
    val STARRED_PROMPT_IDS = stringSetPreferencesKey("starred_prompt_ids")
}
