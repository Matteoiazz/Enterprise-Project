package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.CartItemDTO

@Composable
fun CartItemCard(item: CartItemDTO) {
    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Nota: senza una chiamata al catalogo qui abbiamo solo l'id
                    // dell'articolo, non il suo titolo/immagine.
                    Text(
                        text = "Articolo #${item.catalogItemId}",
                        style = CatalogType.TitleCompact,
                        color = CatalogColors.Ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quantità: ${item.quantity}",
                        style = CatalogType.Body,
                        color = CatalogColors.InkMuted
                    )

                    if (item.checkIn != null && item.checkOut != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = CatalogColors.InkMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.checkIn} → ${item.checkOut}",
                                style = CatalogType.Caption,
                                color = CatalogColors.InkMuted
                            )
                        }
                    }
                }

                Text(
                    text = "€${"%.2f".format(item.priceAtAdded * item.quantity)}",
                    style = CatalogType.Price,
                    color = CatalogColors.AccentDark
                )
            }
        }
    }
}
