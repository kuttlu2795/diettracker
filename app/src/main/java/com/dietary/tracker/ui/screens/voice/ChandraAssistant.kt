package com.dietary.tracker.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

sealed class ChandraCommand {
    data object HeartRate: ChandraCommand(); data object Steps: ChandraCommand(); data object Calories: ChandraCommand(); data object Water: ChandraCommand()
    data object Weight: ChandraCommand(); data object Sleep: ChandraCommand(); data object SpO2: ChandraCommand(); data object BloodPressure: ChandraCommand()
    data object WatchSync: ChandraCommand(); data object Fasting: ChandraCommand(); data object Protein: ChandraCommand(); data object Diary: ChandraCommand(); data object Goals: ChandraCommand()
    data class AddFood(val name:String,val kcal:Int): ChandraCommand(); data class AddExercise(val name:String,val minutes:Int): ChandraCommand(); data class Unknown(val raw:String): ChandraCommand()
}

class ChandraAssistant(private val context: Context, private val onCommand:(ChandraCommand)->Unit, private val onState:(Boolean)->Unit) : TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = TextToSpeech(context,this)
    private var ttsReady=false
    override fun onInit(status:Int) { ttsReady=status==TextToSpeech.SUCCESS; if(ttsReady) tts?.language=Locale("en","IN") }
    fun listen() {
        if(!SpeechRecognizer.isRecognitionAvailable(context)) { speak("Voice recognition is not available on this phone."); return }
        if(recognizer==null) recognizer=SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object:RecognitionListener {
            override fun onReadyForSpeech(p:Bundle?){onState(true)}; override fun onBeginningOfSpeech(){onState(true)}; override fun onRmsChanged(v:Float){}
            override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){onState(false)}; override fun onPartialResults(b:Bundle?){}; override fun onEvent(t:Int,p:Bundle?){}
            override fun onError(e:Int){onState(false);speak("Sorry, I couldn't understand that. Please try again.")}
            override fun onResults(r:Bundle?){onState(false); val q=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); handle(q)}
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN"); putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en-IN") })
    }
    fun handle(raw:String) { val q=raw.lowercase(Locale.getDefault()); val c=when{
        q.contains("heart")||q.contains("pulse")||q.contains("heartbeat")||q.contains("ஹார்ட்") -> ChandraCommand.HeartRate
        q.contains("step")||q.contains("நடந்த")||q.contains("ஸ்டெப்") -> ChandraCommand.Steps
        q.contains("calorie")||q.contains("kcal")||q.contains("கலோரி") -> ChandraCommand.Calories
        q.contains("water")||q.contains("glass")||q.contains("தண்ண") -> ChandraCommand.Water
        q.contains("weight")||q.contains("எடை") -> ChandraCommand.Weight
        q.contains("sleep")||q.contains("தூக்கம்") -> ChandraCommand.Sleep
        q.contains("spo2")||q.contains("oxygen") -> ChandraCommand.SpO2
        q.contains("blood pressure")||q=="bp"||q.contains("pressure") -> ChandraCommand.BloodPressure
        q.contains("sync")||q.contains("watch")||q.contains("goboult") -> ChandraCommand.WatchSync
        q.contains("fasting")||q.contains("fast") -> ChandraCommand.Fasting
        q.contains("banana") -> ChandraCommand.AddFood("Banana",105); q.contains("dosa") -> ChandraCommand.AddFood("Dosa",168); q.contains("idli") -> ChandraCommand.AddFood("Idli",58); q.contains("egg") -> ChandraCommand.AddFood("Egg",78)
        q.contains("walk")||q.contains("walking") -> ChandraCommand.AddExercise("Walking",30)
        q.contains("protein")||q.contains("புரத") -> ChandraCommand.Protein
        q.contains("diary")||q.contains("சாப்பிட்ட") -> ChandraCommand.Diary
        q.contains("goal")||q.contains("target") -> ChandraCommand.Goals
        else -> ChandraCommand.Unknown(raw)
    }; onCommand(c) }
    fun speak(text:String) { if(ttsReady) tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"chandra") }
    fun destroy(){recognizer?.destroy();recognizer=null;tts?.stop();tts?.shutdown();tts=null}
}
