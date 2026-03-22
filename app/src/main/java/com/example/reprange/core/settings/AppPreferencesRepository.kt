package com.example.reprange.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(
    private val context: Context
) {
    val targetRepsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TARGET_REPS_KEY] ?: DEFAULT_TARGET_REPS
    }

    suspend fun setTargetReps(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_REPS_KEY] = value
        }
    }

    companion object {
        const val DEFAULT_TARGET_REPS = 10
        private val TARGET_REPS_KEY = intPreferencesKey("target_reps")
    }
}
