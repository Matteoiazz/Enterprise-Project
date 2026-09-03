package com.tripify.tripify_android.catalog.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.data.TokenManager

// Tasso fisso, nessun servizio di cambio reale collegato.
private const val EUR_TO_USD_RATE = 1.08

object CatalogPriceFormatter {
    fun symbolFor(currency: String) = if (currency == "USD") "$" else "€"

    fun convert(amountEur: Double, currency: String): Double =
        if (currency == "USD") amountEur * EUR_TO_USD_RATE else amountEur

    fun format(amountEur: Double, currency: String): String =
        "${symbolFor(currency)} ${convert(amountEur, currency).toInt()}"
}

/** Valuta scelta nelle Impostazioni, osservata in tempo reale (stesso storage del resto dell'app). */
@Composable
fun rememberCatalogCurrency(): State<String> {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    return tokenManager.currencyFlow.collectAsState(initial = "EUR")
}

/** Ricostruisce lo stesso formato di CatalogItem.price ma nella valuta scelta. */
fun CatalogItem.formattedPrice(currency: String): String = when (this) {
    is CatalogItem.Flight -> "Da ${CatalogPriceFormatter.format(priceValue.toDouble(), currency)}"
    is CatalogItem.Hotel -> "Da ${CatalogPriceFormatter.format(priceValue.toDouble(), currency)}/notte"
    is CatalogItem.Excursion -> CatalogPriceFormatter.format(priceValue.toDouble(), currency)
}
