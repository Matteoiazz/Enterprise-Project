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
import androidx.compose.material.icons.Icons
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

/**
 * Bottone/card premuto: leggero scale-down al tocco, coerente su tutte le card cliccabili.
 */
fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
        modifier = modifier.fillMaxWidth().height(height).pressScale(onClick)
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

/**
 * Fila di 5 stelle con supporto alla mezza stella. Se rating <= 0 non renderizza nulla:
 * i chiamanti devono comunque guardare rating > 0 se vogliono nascondere l'intero blocco rating.
 */
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
