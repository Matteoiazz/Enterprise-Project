package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun catalogDatePickerColors(): DatePickerColors = DatePickerDefaults.colors(
    containerColor = CatalogColors.Surface,
    titleContentColor = CatalogColors.InkMuted,
    headlineContentColor = CatalogColors.Ink,
    weekdayContentColor = CatalogColors.InkMuted,
    subheadContentColor = CatalogColors.Ink,
    navigationContentColor = CatalogColors.AccentDark,
    yearContentColor = CatalogColors.InkMuted,
    currentYearContentColor = CatalogColors.AccentDark,
    selectedYearContentColor = Color.White,
    selectedYearContainerColor = CatalogColors.AccentDark,
    dayContentColor = CatalogColors.Ink,
    disabledDayContentColor = CatalogColors.InkSubtle,
    selectedDayContentColor = Color.White,
    disabledSelectedDayContentColor = Color.White,
    selectedDayContainerColor = CatalogColors.AccentDark,
    disabledSelectedDayContainerColor = CatalogColors.AccentDark.copy(alpha = 0.4f),
    todayContentColor = CatalogColors.AccentDark,
    todayDateBorderColor = CatalogColors.AccentDark,
    dayInSelectionRangeContainerColor = CatalogColors.AccentSoft,
    dayInSelectionRangeContentColor = CatalogColors.AccentDark,
    dividerColor = CatalogColors.Hairline
)

/**
 * Bottone/card premuto: leggero scale-down al tocco, coerente su tutte le card cliccabili.
 */
fun Modifier.pressScale(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun CatalogImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .crossfade(320)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.background(CatalogColors.SurfaceMuted)
    )
}

@Composable
fun PhotoScrim(
    modifier: Modifier = Modifier,
    startY: Float = 120f,
    maxAlpha: Float = 0.92f
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to CatalogColors.Scrim.copy(alpha = maxAlpha * 0.4f),
                1f to CatalogColors.Scrim.copy(alpha = maxAlpha),
                startY = startY
            )
        )
    )
}

@Composable
fun PhotoEyebrow(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CatalogShapes.Pill)
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CatalogShapes.Pill)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = text.uppercase(), style = CatalogType.Overline, color = Color.White.copy(alpha = 0.96f), maxLines = 1)
    }
}

@Composable
fun PriceBadge(price: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CatalogShapes.Pill)
            .background(Color.White.copy(alpha = 0.96f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = price, style = CatalogType.Price, color = CatalogColors.Ink, maxLines = 1)
    }
}

@Composable
fun PhotoMeta(
    icon: ImageVector,
    text: String,
    tint: Color = Color.White.copy(alpha = 0.78f),
    textColor: Color = Color.White.copy(alpha = 0.94f)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = text, style = CatalogType.Meta, color = textColor, maxLines = 1)
    }
}

@Composable
fun PhotoCard(
    imageUrl: String,
    eyebrow: String,
    price: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 230.dp,
    titleStyle: androidx.compose.ui.text.TextStyle = CatalogType.CardTitle,
    titleMaxLines: Int = 2,
    meta: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = CatalogShapes.Card,
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth().height(height).pressScale(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CatalogImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            PhotoScrim(modifier = Modifier.fillMaxSize(), startY = 110f)

            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PhotoEyebrow(eyebrow, modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.width(6.dp))
                PriceBadge(price)
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Text(text = title, style = titleStyle, color = Color.White, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(9.dp))
                meta()
            }
        }
    }
}

/**
 * X per svuotare un campo di ricerca/testo con un tap, mostrata solo quando c'è qualcosa da
 * cancellare. Riusata in tutti i campi di ricerca del modulo Catalog.
 */
@Composable
fun ClearFieldButton(onClear: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClear, modifier = modifier.size(32.dp)) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Cancella",
            tint = CatalogColors.InkSubtle,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun CatalogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        placeholder = { Text(text = placeholder, style = CatalogType.Label, color = CatalogColors.InkSubtle) },
        leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(18.dp)) },
        trailingIcon = { if (value.isNotEmpty()) ClearFieldButton(onClear = { onValueChange("") }) },
        textStyle = CatalogType.Label.copy(color = CatalogColors.Ink),
        singleLine = true,
        shape = CatalogShapes.Field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CatalogColors.AccentDark,
            unfocusedBorderColor = CatalogColors.Hairline,
            focusedContainerColor = CatalogColors.Surface,
            unfocusedContainerColor = CatalogColors.SurfaceMuted,
            cursorColor = CatalogColors.AccentDark
        ),
        modifier = modifier.fillMaxWidth().height(54.dp)
    )
}

fun ratingLabel(rating: Double): String = when {
    rating >= 4.5 -> "Eccellente"
    rating >= 4.0 -> "Ottimo"
    rating >= 3.5 -> "Molto buono"
    rating >= 3.0 -> "Buono"
    rating > 0.0 -> "Discreto"
    else -> "Nessuna recensione"
}


@Composable
fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: Dp = 13.dp,
    filledTint: Color = CatalogColors.Gold,
    emptyTint: Color = CatalogColors.Hairline
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { index ->
            val diff = rating - index
            val icon = when {
                diff >= 1.0 -> Icons.Filled.Star
                diff >= 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (diff > 0) filledTint else emptyTint,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -800f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1300, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmerOffset"
    )
    return Brush.linearGradient(
        colors = listOf(CatalogColors.SurfaceMuted, Color.White, CatalogColors.SurfaceMuted),
        start = Offset(offset, 0f),
        end = Offset(offset + 420f, 420f)
    )
}

@Composable
fun CatalogCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth().clip(CatalogShapes.Card).border(1.dp, CatalogColors.Hairline, CatalogShapes.Card).background(CatalogColors.Surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(166.dp).background(brush))
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.62f).height(15.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Box(modifier = Modifier.fillMaxWidth(0.38f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
    }
}

/**
 * Coppia di campi check-in/check-out con DatePicker a tema, condivisa tra il form di
 * ricerca hotel e il dettaglio hotel — così le date scelte in un posto si possono
 * ripassare pari pari all'altro invece di farle riscegliere da capo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeRow(
    checkInMillis: Long?,
    checkOutMillis: Long?,
    onCheckInChange: (Long?) -> Unit,
    onCheckOutChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN) }
    val todayMillis = remember { LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }

    fun label(millis: Long?, placeholder: String): String =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(formatter) } ?: placeholder

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateField(label = "Check-in", value = label(checkInMillis, "Scegli data"), isSet = checkInMillis != null, onClick = { showCheckInPicker = true }, modifier = Modifier.weight(1f))
        DateField(label = "Check-out", value = label(checkOutMillis, "Scegli data"), isSet = checkOutMillis != null, onClick = { showCheckOutPicker = true }, modifier = Modifier.weight(1f))
    }

    if (showCheckInPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = checkInMillis ?: todayMillis,
            selectableDates = remember { object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis } }
        )
        DatePickerDialog(
            onDismissRequest = { showCheckInPicker = false },
            colors = catalogDatePickerColors(),
            confirmButton = { TextButton(onClick = { onCheckInChange(state.selectedDateMillis); showCheckInPicker = false }) { Text("Conferma", color = CatalogColors.AccentDark) } },
            dismissButton = { TextButton(onClick = { showCheckInPicker = false }) { Text("Annulla", color = CatalogColors.InkMuted) } }
        ) { DatePicker(state = state, colors = catalogDatePickerColors()) }
    }

    if (showCheckOutPicker) {
        val minMillis = checkInMillis?.let { it + 24L * 60 * 60 * 1000 } ?: todayMillis
        val state = rememberDatePickerState(
            initialSelectedDateMillis = checkOutMillis?.takeIf { it >= minMillis } ?: minMillis,
            selectableDates = remember(minMillis) { object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis } }
        )
        DatePickerDialog(
            onDismissRequest = { showCheckOutPicker = false },
            colors = catalogDatePickerColors(),
            confirmButton = { TextButton(onClick = { onCheckOutChange(state.selectedDateMillis); showCheckOutPicker = false }) { Text("Conferma", color = CatalogColors.AccentDark) } },
            dismissButton = { TextButton(onClick = { showCheckOutPicker = false }) { Text("Annulla", color = CatalogColors.InkMuted) } }
        ) { DatePicker(state = state, colors = catalogDatePickerColors()) }
    }
}

@Composable
private fun DateField(label: String, value: String, isSet: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = BorderStroke(1.dp, if (isSet) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = modifier.pressScale(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = CatalogType.Caption, color = CatalogColors.InkMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = if (isSet) CatalogColors.AccentDark else CatalogColors.InkSubtle, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(value, style = CatalogType.BodyStrong, color = if (isSet) CatalogColors.Ink else CatalogColors.InkSubtle)
            }
        }
    }
}
