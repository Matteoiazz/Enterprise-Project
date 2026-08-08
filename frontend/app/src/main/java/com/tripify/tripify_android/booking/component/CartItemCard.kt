package com.tripify.tripify_android.booking.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.data.model.CartItemDTO

@Composable
fun CartItemCard(item: CartItemDTO) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nota: Se nel DTO hai aggiunto il nome dell'escursione/volo stampalo qui!
            Text(
                text = "ID Articolo: ${item.catalogItemId}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Quantità: ${item.quantity}")
            Text(
                text = "Prezzo: €${item.priceAtAdded}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}