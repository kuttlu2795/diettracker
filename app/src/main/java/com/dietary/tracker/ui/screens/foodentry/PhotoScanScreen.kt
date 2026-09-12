package com.dietary.tracker.ui.screens.foodentry

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dietary.tracker.network.AiEstimateOutcome
import com.dietary.tracker.network.AiNutritionEstimator
import com.dietary.tracker.network.NutritionResult
import com.dietary.tracker.ocr.NutritionLabelParser
import kotlinx.coroutines.launch

private enum class PhotoMode { LABEL_OCR, MEAL_AI }

@Composable
fun PhotoScanScreen(
    onParsed: (NutritionResult) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiEstimator = remember { AiNutritionEstimator() }
    var mode by remember { mutableStateOf(PhotoMode.LABEL_OCR) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            bitmap = bmp
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) bitmap = bmp
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Scan a Photo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == PhotoMode.LABEL_OCR,
                onClick = { mode = PhotoMode.LABEL_OCR; bitmap = null; errorMsg = null },
                label = { Text("Nutrition Label") }
            )
            FilterChip(
                selected = mode == PhotoMode.MEAL_AI,
                onClick = { mode = PhotoMode.MEAL_AI; bitmap = null; errorMsg = null },
                label = { Text("✨ Meal Photo (AI)") }
            )
        }
        Spacer(Modifier.height(12.dp))

        Text(
            if (mode == PhotoMode.LABEL_OCR)
                "Take or choose a clear photo of the packaged food's nutrition facts panel - text is read directly off the label."
            else
                "Take or choose a photo of your plate/meal - AI will identify what's on it and estimate nutrition for the portion shown, even with no label at all.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                Text("Take Photo")
            }
            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                Text("Choose from Gallery")
            }
        }

        Spacer(Modifier.height(16.dp))

        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Selected photo",
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    isProcessing = true
                    errorMsg = null
                    scope.launch {
                        if (mode == PhotoMode.LABEL_OCR) {
                            try {
                                val result = NutritionLabelParser.parse(bmp, fallbackName = "Scanned Product")
                                isProcessing = false
                                onParsed(result)
                            } catch (e: Exception) {
                                isProcessing = false
                                errorMsg = "Couldn't read the label. Try a clearer, well-lit photo."
                            }
                        } else {
                            when (val outcome = aiEstimator.estimateFromImage(bmp)) {
                                is AiEstimateOutcome.Success -> {
                                    isProcessing = false
                                    onParsed(outcome.result)
                                }
                                is AiEstimateOutcome.Error -> {
                                    isProcessing = false
                                    errorMsg = outcome.message
                                }
                                AiEstimateOutcome.MissingApiKey -> {
                                    isProcessing = false
                                    errorMsg = "Add a free Gemini API key in local.properties to enable AI meal-photo recognition."
                                }
                            }
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isProcessing && mode == PhotoMode.LABEL_OCR -> "Reading label..."
                        isProcessing -> "✨ Recognizing meal..."
                        mode == PhotoMode.LABEL_OCR -> "Extract Nutrition Info"
                        else -> "✨ Recognize Meal & Estimate Nutrition"
                    }
                )
            }
        }

        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (isProcessing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
