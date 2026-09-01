package com.tripify.tripify_android.organizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.CatalogPriceFormatter
import com.tripify.tripify_android.data.model.ReceivedBookingLineDto
import kotlin.math.max

private val SlicePalette = listOf(
    CatalogColors.AccentDark,
    CatalogColors.Accent,
    CatalogColors.AccentLight,
    CatalogColors.Gold,
    Color(0xFF2D6A4F),
    Color(0xFF95D5B2)
)

private data class RevenueSlice(val label: String, val amount: Double, val color: Color)

@Composable
fun ReceivedBookingsSummary(
    lines: List<ReceivedBookingLineDto>,
    titleFor: (Long) -> String?,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (lines.isEmpty()) return

    val confirmed = lines.filter { it.status.equals("CONFIRMED", ignoreCase = true) }
    val pending = lines.filter { it.status.equals("PENDING", ignoreCase = true) }

    val confirmedTotal = confirmed.sumOf { it.price }
    val pendingTotal = pending.sumOf { it.price }
    val bookingCount = lines.map { it.bookingId }.distinct().size

    val byItem = confirmed
        .groupBy { it.catalogItemId }
        .map { (id, group) -> id to group.sumOf { it.price } }
        .sortedByDescending { it.second }

    val topSlices = byItem.take(5).mapIndexed { index, (id, amount) ->
        RevenueSlice(
            label = titleFor(id) ?: "Annuncio #$id",
            amount = amount,
            color = SlicePalette[index % SlicePalette.size]
        )
    }
    val restAmount = byItem.drop(5).sumOf { it.second }
    val slices = if (restAmount > 0.0) {
        topSlices + RevenueSlice("Altri", restAmount, CatalogColors.InkSubtle)
    } else topSlices

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CatalogShapes.Card)
            .background(CatalogColors.Surface)
            .border(BorderStroke(1.dp, CatalogColors.Hairline), CatalogShapes.Card)
            .padding(18.dp)
    ) {
        Text("INCASSO CONFERMATO", style = CatalogType.Overline, color = CatalogColors.InkMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            CatalogPriceFormatter.format(confirmedTotal, currency),
            style = CatalogType.PriceLarge,
            color = CatalogColors.Ink
        )

        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(
                label = "In attesa",
                value = CatalogPriceFormatter.format(pendingTotal, currency),
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                label = "Prenotazioni",
                value = bookingCount.toString(),
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                label = "Righe",
                value = lines.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        if (slices.isNotEmpty() && confirmedTotal > 0.0) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(
                    values = slices.map { it.color to it.amount.toFloat() },
                    modifier = Modifier.size(104.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    slices.forEach { slice ->
                        val pct = if (confirmedTotal > 0) (slice.amount / confirmedTotal * 100).toInt() else 0
                        LegendRow(
                            color = slice.color,
                            label = slice.label,
                            trailing = "$pct%"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CatalogShapes.Field)
            .background(CatalogColors.SurfaceMuted)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Text(value, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1)
        Text(label, style = CatalogType.Caption, color = CatalogColors.InkMuted)
    }
}

@Composable
private fun LegendRow(color: Color, label: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = CatalogType.Caption,
            color = CatalogColors.Ink,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(trailing, style = CatalogType.LabelStrong, color = CatalogColors.InkMuted)
    }
}

@Composable
fun DonutChart(
    values: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 34f
) {
    val total = values.sumOf { it.second.toDouble() }.toFloat()
    Canvas(modifier = modifier) {
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = CatalogColors.Hairline,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        if (total <= 0f) return@Canvas

        var startAngle = -90f
        values.forEach { (color, value) ->
            val sweep = max(0f, value / total * 360f)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep - 1.5f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}
