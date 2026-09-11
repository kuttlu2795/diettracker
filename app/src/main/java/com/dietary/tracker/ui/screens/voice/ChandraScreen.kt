package com.dietary.tracker.ui.screens.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dietary.tracker.health.HealthConnectManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ChandraScreen(onBack:()->Unit={}) {
    val context=LocalContext.current; val scope=rememberCoroutineScope(); val hc=remember{HealthConnectManager(context)}
    var listening by remember{mutableStateOf(false)}; var response by remember{mutableStateOf("Hi! I'm Chandra. Ask me about your health, food, calories, steps, water or exercise.")}
    var last by remember{mutableStateOf("Ready to listen")}; var steps by remember{mutableStateOf<Long?>(null)}; var hr by remember{mutableStateOf<Long?>(null)}; var weight by remember{mutableStateOf<Double?>(null)}; var sleep by remember{mutableStateOf<Double?>(null)}; var spo2 by remember{mutableStateOf<Double?>(null)}; var bp by remember{mutableStateOf<Pair<Double,Double>?>(null)}
    var assistant by remember{mutableStateOf<ChandraAssistant?>(null)}
    var alwaysListening by remember { mutableStateOf(ChandraVoiceSettings.isAlwaysListeningEnabled(context)) }
    fun refresh(){scope.launch{steps=hc.todaySteps();hr=hc.latestHeartRate();weight=hc.latestWeightKg();sleep=hc.latestSleepHours();spo2=hc.latestOxygenSaturation();bp=hc.latestBloodPressure()}}
    LaunchedEffect(Unit){refresh()}
    DisposableEffect(Unit){val a=ChandraAssistant(context,{cmd->
        scope.launch{
            when(cmd){
                ChandraCommand.HeartRate -> response=hr?.let{"உங்களுடைய current heart rate ${it} BPM."}?:"Heart rate data கிடைக்கவில்லை. Health Connect permission/check your watch sync."
                ChandraCommand.Steps -> response="இன்னைக்கு ${(steps?:0L).toString()} steps நடந்திருக்கீங்க."
                ChandraCommand.Weight -> response=weight?.let{"Current weight ${String.format(Locale.US,"%.1f",it)} kg."}?:"Weight data கிடைக்கவில்லை."
                ChandraCommand.Sleep -> response=sleep?.let{"நேத்து sleep ${String.format(Locale.US,"%.1f",it)} hours."}?:"Sleep data கிடைக்கவில்லை."
                ChandraCommand.SpO2 -> response=spo2?.let{"Latest SpO₂ wellness reading ${String.format(Locale.US,"%.0f",it)} percent."}?:"SpO₂ data கிடைக்கவில்லை."
                ChandraCommand.BloodPressure -> response=bp?.let{"Latest BP wellness reading ${String.format(Locale.US,"%.0f",it.first)} over ${String.format(Locale.US,"%.0f",it.second)}."}?:"Blood pressure data கிடைக்கவில்லை."
                ChandraCommand.Calories -> response="Today's calorie balance will be read from your Diet Tracker diary."
                ChandraCommand.Water -> response="Today's water progress will be read from your Water tracker."
                ChandraCommand.WatchSync -> {refresh();response="Wearable data refresh started."}
                ChandraCommand.Fasting -> response="Fasting control opened."
                ChandraCommand.Protein -> response="Today's protein progress will be read from your food diary."
                ChandraCommand.Diary -> response="I can read today's logged foods from your Diet Tracker diary."
                ChandraCommand.Goals -> response="Your goals are available in Profile & Goals."
                is ChandraCommand.AddFood -> response="${cmd.name} added command recognized. Connect this action to the food repository to persist it."
                is ChandraCommand.AddExercise -> response="${cmd.minutes} minutes ${cmd.name} command recognized."
                is ChandraCommand.Unknown -> response="இந்த command இன்னும் configured இல்லை. Voice Commands menu-la custom alias add பண்ணலாம்."
            }
            last="Command received"; assistant?.speak(response.replace("<b>","").replace("</b>",""))
        }
    },{listening=it});assistant=a;onDispose{a.destroy()}}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack){Text("Back")};Text("Chandra Voice Assistant",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
        Spacer(Modifier.height(12.dp));Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("🎙️",style=MaterialTheme.typography.displaySmall);Text(if(listening)"Listening…"else"Hi Chandra / Chandra",fontWeight=FontWeight.Bold);Text(last,style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(8.dp));Button(onClick{assistant?.listen()}){Text(if(listening)"Listening…"else"Start Listening")};Spacer(Modifier.height(10.dp));Text(response)}}
        Spacer(Modifier.height(12.dp));Text("Live Health Data",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));
        LazyColumn{items(listOf("❤️ Heart Rate: ${hr?.let{"${it} BPM"}?:"No data"}","👣 Steps: ${steps?:0L}","⚖️ Weight: ${weight?.let{String.format(Locale.US,"%.1f kg",it)}?:"No data"}","😴 Sleep: ${sleep?.let{String.format(Locale.US,"%.1f h",it)}?:"No data"}","🫁 SpO₂: ${spo2?.let{String.format(Locale.US,"%.0f%%",it)}?:"No data"}","🩺 BP: ${bp?.let{"${it.first.toInt()} / ${it.second.toInt()}"}?:"No data"}")){Text(it,Modifier.padding(vertical=7.dp))}}
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("🔊 Always listen for “Hi Chandra”", fontWeight = FontWeight.Bold)
                        Text(if (alwaysListening) "ON — Chandra can listen while the phone is locked." else "OFF — background microphone is stopped.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = alwaysListening, onCheckedChange = { enabled ->
                        alwaysListening = enabled
                        ChandraVoiceSettings.setAlwaysListeningEnabled(context, enabled)
                        val intent = Intent(context, ChandraWakeService::class.java).setAction(if (enabled) ChandraWakeService.ACTION_START else ChandraWakeService.ACTION_STOP)
                        if (enabled) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
                    })
                }
                if (alwaysListening) {
                    Spacer(Modifier.height(8.dp))
                    Text("Battery note: Chandra uses a foreground microphone service while this switch is ON. Android will show an ongoing notification.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Voice Commands",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Heart rate • Steps • Calories • Water • Weight • Sleep • SpO₂ • BP • Food • Exercise • Fasting • Watch Sync • Diary • Goals",style=MaterialTheme.typography.bodyMedium)
    }
}
