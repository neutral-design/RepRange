package com.example.reprange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.reprange.core.data.OfflineWorkoutRepository
import com.example.reprange.core.data.local.AppDatabase
import com.example.reprange.features.diary.presentation.DiaryViewModelFactory
import com.example.reprange.features.diary.ui.DiaryRoute
import com.example.reprange.ui.theme.RepRangeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = OfflineWorkoutRepository(AppDatabase.getInstance(applicationContext).workoutDao())
        val factory = DiaryViewModelFactory(repository)

        setContent {
            RepRangeTheme {
                DiaryRoute(factory = factory)
            }
        }
    }
}
