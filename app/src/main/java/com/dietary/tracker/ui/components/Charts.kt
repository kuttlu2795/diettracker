package com.dietary.tracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarChartEntry(val label: String, val value: Float)

@Composable
fun AnimatedBarChart(
    entries: List<BarChartEntry>,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    valueSuffix: String = ""
) {
    val maxValue = (entries.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)
    var animate by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(entries) { animate = true }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val targetFraction = if (animate) (entry.value / maxValue) else 0f
            val fraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 700),
                label = "bar"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (entry.value > 0) "${entry.value.toInt()}$valueSuffix" else "",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((130 * fraction).dp.coerceAtLeast(2.dp))
                        .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.label,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun Modifier.background(color: Color, shape: androidx.compose.foundation.shape.RoundedCornerShape) =
    this.then(androidx.compose.foundation.background(color = color, shape = shape))

data class LinePoint(val label: String, val value: Float)

@Composable
fun AnimatedLineChart(
    points: List<LinePoint>,
    color: Color = MaterialTheme.colorScheme.tertiary,
    modifier: Modifier = Modifier
) {
    var animate by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(points) { animate = true }
    val progress by animateFloatAsState(
        targetValue = if (animate) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "line"
    )

    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Not enough data yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    val maxVal = points.maxOf { it.value }
    val minVal = points.minOf { it.value }
    val range = (maxVal - minVal).coerceAtLeast(0.1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 8.dp)
        ) {
            val stepX = size.width / (points.size - 1)
            val fullPath = points.mapIndexed { index, p ->
                val x = index * stepX
                val normalized = (p.value - minVal) / range
                val y = size.height - (normalized * size.height)
                Offset(x, y)
            }
            val visibleCount = (fullPath.size * progress).toInt().coerceIn(1, fullPath.size)
            val visiblePath = fullPath.take(visibleCount)

            for (i in 0 until visiblePath.size - 1) {
                drawLine(
                    color = color,
                    start = visiblePath[i],
                    end = visiblePath[i + 1],
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            visiblePath.forEach { point ->
                drawCircle(color = color, radius = 8f, center = point)
                drawCircle(color = Color.White, radius = 4f, center = point)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach {
                Text(
                    it.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
