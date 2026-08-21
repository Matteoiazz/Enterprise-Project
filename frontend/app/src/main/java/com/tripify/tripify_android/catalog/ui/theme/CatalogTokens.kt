package com.tripify.tripify_android.catalog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette Catalog: autosufficiente, non dipende da core/theme (che oggi ha due verdi
// diversi tra Color.kt e TripifyApp.kt). L'accento verde-pino nasce dallo stesso tono
// già usato per il marker della mappa in DetailScreen (0x1B4332), qui reso ufficiale.
object CatalogColors {
    val Ink = Color(0xFF14201D)
    val InkMuted = Color(0xFF5F6F6B)
    val InkSubtle = Color(0xFF8A9995)

    val Hairline = Color(0xFFE2E9E6)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF1F5F3)
    val Background = Color(0xFFF7F5EF)

    // Famiglia verde-pino: dal più scuro (CTA, testo su chiaro) al più chiaro (tint di sfondo).
    val AccentDark = Color(0xFF1B4332)
    val Accent = Color(0xFF40916C)
    val AccentLight = Color(0xFF74C69D)
    val AccentSoft = Color(0xFFE7F2EB)

    val Gold = Color(0xFFC38A2E)
    val GoldSoft = Color(0xFFF6ECD9)
    val Alert = Color(0xFFC1483C)
    val AlertOnDark = Color(0xFFFFB4A6)
    val AlertSoft = Color(0xFFF7E4E1)

    val Scrim = Color(0xFF0B1A16)
}

object CatalogShapes {
    val Card = RoundedCornerShape(18.dp)
    val Field = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(10.dp)
    val Badge = RoundedCornerShape(8.dp)
    val Pill = RoundedCornerShape(50)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

object CatalogSpacing {
    val Gutter = 20.dp
    val ListGap = 14.dp
    val Section = 32.dp
}
