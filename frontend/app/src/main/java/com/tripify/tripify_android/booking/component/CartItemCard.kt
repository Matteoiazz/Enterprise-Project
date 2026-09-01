package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.booking.util.currencySymbol
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.CartItemDTO

@Composable
fun CartItemCard(
    item: CartItemDTO,
    catalogViewModel: CatalogViewModel,
    selected: Boolean = true,
    onToggleSelected: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    // false nel riepilogo di sola lettura del checkout: lì la selezione è già
    // stata fatta in CartScreen, non ha senso poterla cambiare o rimuovere articoli.
    showControls: Boolean = true
) {
    var resolved by remember(item.catalogItemId) { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(item.catalogItemId) {
        resolved = catalogViewModel.getOrFetchItem(item.catalogItemId.toInt())
    }

    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            if (showControls) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelected() },
                    colors = CheckboxDefaults.colors(checkedColor = CatalogColors.AccentDark)
                )
            }

            AsyncImage(
                model = resolved?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolved?.title ?: "Articolo #${item.catalogItemId}",
                    style = CatalogType.TitleCompact,
                    color = CatalogColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

            Column(horizontalAlignment = Alignment.End) {
                // Prezzo dell'articolo nella SUA valuta originale, non convertita:
                // la conversione riguarda solo il totale aggregato (vedi
                // CartScreen/CheckoutScreen), qui si mostra esattamente cosa costa.
                Text(
                    text = "${currencySymbol(item.currency ?: "EUR")}${"%.2f".format(item.priceAtAdded * item.quantity)}",
                    style = CatalogType.Price,
                    color = CatalogColors.AccentDark
                )
                if (showControls) {
                    IconButton(onClick = onRemoveClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Rimuovi dal carrello",
                            tint = CatalogColors.Alert,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
