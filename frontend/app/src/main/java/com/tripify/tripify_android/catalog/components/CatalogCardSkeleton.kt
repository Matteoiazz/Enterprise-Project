package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

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
            .crossfade(260)
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
                0.55f to CatalogColors.Scrim.copy(alpha = maxAlpha * 0.45f),
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
            .clip(CatalogShapes.Badge)
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(text = text.uppercase(), style = CatalogType.Overline, color = Color.White.copy(alpha = 0.95f), maxLines = 1)
    }
}

@Composable
fun PriceBadge(price: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CatalogShapes.Badge)
            .background(Color.White)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(text = price, style = CatalogType.Price, color = CatalogColors.Ink, maxLines = 1)
    }
}

@Composable
fun PhotoMeta(
    icon: ImageVector,
    text: String,
    tint: Color = Color.White.copy(alpha = 0.72f),
    textColor: Color = Color.White.copy(alpha = 0.92f)
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
    height: Dp = 228.dp,
    meta: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = CatalogShapes.Card,
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth().height(height).clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CatalogImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            PhotoScrim(modifier = Modifier.fillMaxSize(), startY = 110f)

            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PhotoEyebrow(eyebrow)
                PriceBadge(price)
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Text(text = title, style = CatalogType.CardTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(9.dp))
                meta()
            }
        }
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
        leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
        textStyle = CatalogType.Label.copy(color = CatalogColors.Ink),
        singleLine = true,
        shape = CatalogShapes.Field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TripifyDarkGreen,
            unfocusedBorderColor = CatalogColors.Hairline,
            focusedContainerColor = CatalogColors.Surface,
            unfocusedContainerColor = CatalogColors.SurfaceMuted,
            cursorColor = TripifyDarkGreen
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