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
        ActivityResultContracts.RequestPermission()
    ) { /* result handled implicitly; sensor just won't report without it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DietTrackerApp
        stepCounterManager = StepCounterManager(this, app.repository, lifecycleScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        val factory = ViewModelFactory(app.repository, app.nutritionRepository)

        setContent {
            DietTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavGraph(factory = factory)
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
