package com.dietary.tracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dietary.tracker.sensors.StepCounterManager
import com.dietary.tracker.ui.ViewModelFactory
import com.dietary.tracker.ui.navigation.AppNavGraph
import com.dietary.tracker.ui.theme.DietTrackerTheme

class MainActivity : ComponentActivity() {

    private lateinit var stepCounterManager: StepCounterManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* sensor/notification features gracefully degrade when denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("open_chandra", false) == true) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val app = application as DietTrackerApp
        stepCounterManager = StepCounterManager(this, app.repository, lifecycleScope)

        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.RECORD_AUDIO)
        }.toTypedArray()
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions)

        val factory = ViewModelFactory(app.repository, app.nutritionRepository)

        setContent {
            DietTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavGraph(
                        factory = factory,
                        startRoute = if (intent?.getBooleanExtra("open_chandra", false) == true) "chandra" else "dashboard"
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepCounterManager.start()
    }

    override fun onPause() {
        super.onPause()
        stepCounterManager.stop()
    }
}
