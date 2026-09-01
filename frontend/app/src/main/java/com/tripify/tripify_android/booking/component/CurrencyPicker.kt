package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.util.currencySymbol
import com.tripify.tripify_android.booking.util.supportedCartCurrencies
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

// Selettore compatto EUR/USD per la valuta con cui visualizzare il totale:
// riusato sia in CartScreen che in CheckoutScreen, così la scelta fatta nel
// carrello resta visibile (e modificabile) anche al pagamento - è lo stesso
// storage di Impostazioni > Valuta (TokenManager.currencyFlow), quindi non
// serve passare nulla tra le due schermate. Non cambia gli importi realmente
// addebitati (sempre calcolati e verificati lato server nelle valute
// originali degli articoli), solo come vengono mostrati.
@Composable
fun CurrencyPicker(selected: String, onSelect: (String) -> Unit) {
    Text("Mostra il totale in:", style = CatalogType.Caption, color = CatalogColors.InkMuted)
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        supportedCartCurrencies.forEach { currency ->
            val isSelected = currency == selected
            Surface(
                shape = CatalogShapes.Pill,
                color = if (isSelected) CatalogColors.AccentSoft else CatalogColors.Background,
                border = BorderStroke(1.dp, if (isSelected) CatalogColors.AccentDark else CatalogColors.Hairline),
                modifier = Modifier.clickable { onSelect(currency) }
            ) {
                Text(
                    text = "${currencySymbol(currency)} $currency",
                    style = CatalogType.Caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSelected) CatalogColors.AccentDark else CatalogColors.InkMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
