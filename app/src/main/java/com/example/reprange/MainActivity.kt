package com.example.reprange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.reprange.core.data.OfflineWorkoutRepository
import com.example.reprange.core.data.local.AppDatabase
import com.example.reprange.core.settings.AppPreferencesRepository
import com.example.reprange.features.app.ui.RepRangeApp
import com.example.reprange.ui.theme.RepRangeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = OfflineWorkoutRepository(AppDatabase.getInstance(applicationContext).workoutDao())
        val preferencesRepository = AppPreferencesRepository(applicationContext)

        setContent {
            RepRangeTheme {
                RepRangeApp(
                    repository = repository,
                    preferencesRepository = preferencesRepository
                )
            }
        }
    }
}
