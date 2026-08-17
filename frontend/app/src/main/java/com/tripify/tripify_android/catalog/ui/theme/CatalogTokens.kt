package com.tripify.tripify_android.catalog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object CatalogColors {
    val Ink = Color(0xFF14201D)
    val InkMuted = Color(0xFF5F6F6B)
    val InkSubtle = Color(0xFF8A9995)

    val Hairline = Color(0xFFE2E9E6)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF1F5F3)

    val Gold = Color(0xFFC38A2E)
    val Alert = Color(0xFFC1483C)
    val AlertOnDark = Color(0xFFFFB4A6)

    val Scrim = Color(0xFF0B1A16)
}

object CatalogShapes {
    val Card = RoundedCornerShape(18.dp)
    val Field = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(10.dp)
    val Badge = RoundedCornerShape(8.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

object CatalogSpacing {
    val Gutter = 20.dp
    val ListGap = 14.dp
}