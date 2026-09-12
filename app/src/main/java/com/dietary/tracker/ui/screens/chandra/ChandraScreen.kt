package com.dietary.tracker.ui.screens.chandra

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.ui.theme.GreenPrimary
import com.dietary.tracker.ui.theme.OrangeAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChandraScreen(
    viewModel: ChandraViewModel,
    onOpenVoiceCommands: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val recent by viewModel.recentCommands.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    LaunchedEffect(Unit) { viewModel.attach(context) }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            onNavigate(it)
            viewModel.consumeNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Chandra", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Your Diet & Health Voice Assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            IconButton(onClick = onOpenVoiceCommands) {
                Icon(Icons.Filled.Tune, contentDescription = "Voice Commands")
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!settings.assistantEnabled) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Chandra is turned off", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Turn it on below to use voice commands.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MicButton(
                    phase = state.phase,
                    enabled = settings.assistantEnabled,
                    onClick = {
                        if (!hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (settings.assistantEnabled) {
                            viewModel.startListening(context)
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    when (state.phase) {
                        ChandraUiPhase.IDLE -> "Say \"Hi Chandra\" or tap the mic"
                        ChandraUiPhase.LISTENING -> "Listening..."
                        ChandraUiPhase.PROCESSING -> "Thinking..."
                        ChandraUiPhase.RESPONSE -> "Tap the mic to ask again"
                        ChandraUiPhase.ERROR -> "Tap the mic to try again"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        state.heardText?.let { heard ->
            if (state.phase == ChandraUiPhase.PROCESSING || state.phase == ChandraUiPhase.RESPONSE) {
                Text(
                    "\"$heard\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        state.responseText?.let { response ->
            if (state.phase == ChandraUiPhase.RESPONSE || state.phase == ChandraUiPhase.ERROR) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.phase == ChandraUiPhase.ERROR)
                            MaterialTheme.colorScheme.errorContainer else GreenPrimary.copy(alpha = 0.08f)
                    )
                ) {
                    Text(
                        response,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🔊 Always Listen", fontWeight = FontWeight.Bold)
                        Text(
                            "Hands-free \"Hi Chandra\" even when locked/backgrounded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = settings.alwaysListenEnabled,
                        enabled = settings.assistantEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasMicPermission) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.setAlwaysListen(context, enabled)
                            }
                        }
                    )
                }
                if (settings.alwaysListenEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFFFFF7E6), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) {
                        Text(
                            "⚠️ Uses Android's standard speech recognizer in a loop, not a dedicated low-power " +
                                "wake-word engine - battery use is higher than Alexa/Google Assistant, and screen-off " +
                                "reliability depends on your OEM's battery optimizer. See Voice Commands ▸ Settings for details.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A6D00),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Recent Voice Commands", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val fmt = remember { SimpleDateFormat("h:mm a", Locale.US) }
        if (recent.isEmpty()) {
            Text(
                "No commands yet - try tapping the mic and asking something.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(recent) { cmd ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("\"${cmd.heardText}\"", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(cmd.response, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(fmt.format(Date(cmd.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun MicButton(phase: ChandraUiPhase, enabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic-pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )
    val color = when (phase) {
        ChandraUiPhase.LISTENING -> OrangeAccent
        ChandraUiPhase.PROCESSING -> MaterialTheme.colorScheme.tertiary
        ChandraUiPhase.ERROR -> MaterialTheme.colorScheme.error
        else -> GreenPrimary
    }
    val scaleValue = if (phase == ChandraUiPhase.LISTENING) pulse else 1f

    Box(
        modifier = Modifier
            .size(110.dp)
            .scale(scaleValue)
            .background(color.copy(alpha = if (enabled) 1f else 0.4f), CircleShape)
            .then(Modifier.clickableBox(enabled = enabled, onClick = onClick)),
        contentAlignment = Alignment.Center
    ) {
        if (phase == ChandraUiPhase.PROCESSING) {
            CircularProgressIndicator(color = Color.White)
        } else {
            Icon(Icons.Filled.Mic, contentDescription = "Start listening", tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
}

private fun Modifier.clickableBox(enabled: Boolean, onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(enabled = enabled, onClick = onClick))
