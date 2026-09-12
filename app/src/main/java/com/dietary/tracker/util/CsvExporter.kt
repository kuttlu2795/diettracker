package com.dietary.tracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dietary.tracker.data.Repository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /** Builds one combined CSV with a Type column, covering food/water/weight logs. */
    suspend fun buildCsv(repository: Repository): String {
        val sb = StringBuilder()
        sb.append("Type,DateTime,Name/Detail,MealType,Quantity,Calories,ProteinG,CarbsG,SugarG,FatG,FiberG,SodiumMg\n")

        repository.getAllFoodOnce().forEach { e ->
            sb.append("Food,")
            sb.append(dateFmt.format(Date(e.timestamp))).append(',')
            sb.append(csvSafe(e.name)).append(',')
            sb.append(e.mealType).append(',')
            sb.append(e.quantityLabel).append(',')
            sb.append(round1(e.calories)).append(',')
            sb.append(round1(e.protein)).append(',')
            sb.append(round1(e.carbs)).append(',')
            sb.append(round1(e.sugar)).append(',')
            sb.append(round1(e.fat)).append(',')
            sb.append(round1(e.fiber)).append(',')
            sb.append(round1(e.sodium)).append('\n')
        }
        repository.getAllWaterOnce().forEach { e ->
            sb.append("Water,")
            sb.append(dateFmt.format(Date(e.timestamp))).append(',')
            sb.append("${e.amountMl} ml,,,,,,,,,\n")
        }
        repository.getAllWeightOnce().forEach { e ->
            sb.append("Weight,")
            sb.append(dateFmt.format(Date(e.timestamp))).append(',')
            sb.append("${e.weightKg} kg,,,,,,,,,\n")
        }
        return sb.toString()
    }

    /** Writes the CSV to app cache and launches a share/save chooser for it. */
    fun shareCsv(context: Context, csv: String) {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "diet_tracker_export_${System.currentTimeMillis()}.csv"
        val file = File(exportsDir, fileName)
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Diet Tracker data"))
    }

    private fun csvSafe(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value

    private fun round1(value: Double): Double = Calculations.round1(value)
}
