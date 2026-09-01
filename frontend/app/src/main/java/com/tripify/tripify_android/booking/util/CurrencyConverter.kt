package com.tripify.tripify_android.booking.util

// Stesso tasso fisso del catalogo (CatalogPriceFormatting.kt), nessun
// servizio di cambio reale: solo per la visualizzazione, non cambia
// l'importo realmente addebitato.
private const val EUR_TO_USD_RATE = 1.08

// Uniche due valute mostrabili (stessa scelta di Impostazioni > Valuta): un
// articolo senza valuta viene trattato come EUR.
val supportedCartCurrencies = listOf("EUR", "USD")

// Converte un importo dalla sua valuta originale (item.currency) a quella
// scelta per la visualizzazione. A differenza di CatalogPriceFormatter
// converte anche da USD a EUR, non solo il contrario.
fun convertCartAmount(amount: Double, fromCurrency: String?, toCurrency: String): Double {
    val from = fromCurrency ?: "EUR"
    if (from == toCurrency) return amount

    // Passa sempre per EUR come valuta "ponte": per aggiungere altre valute
    // basta il loro tasso verso EUR, senza toccare i chiamanti.
    val amountInEur = if (from == "USD") amount / EUR_TO_USD_RATE else amount
    return if (toCurrency == "USD") amountInEur * EUR_TO_USD_RATE else amountInEur
}

fun currencySymbol(currency: String) = if (currency == "USD") "$" else "€"
