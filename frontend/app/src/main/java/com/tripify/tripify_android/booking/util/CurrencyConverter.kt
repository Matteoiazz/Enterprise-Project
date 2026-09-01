package com.tripify.tripify_android.booking.util

// Stesso tasso fisso usato per il prezzo degli articoli nel catalogo
// (catalog/util/CatalogPriceFormatting.kt): nessun servizio di cambio reale
// collegato, è solo una conversione per la VISUALIZZAZIONE del totale, non
// cambia l'importo realmente addebitato (quello resta sempre booking.totalAmount,
// calcolato lato server sulle valute originali degli articoli).
private const val EUR_TO_USD_RATE = 1.08

// Le uniche due valute che l'app sa mostrare (stessa scelta di Impostazioni >
// Valuta e del catalogo): un articolo con una valuta diversa/mancante viene
// trattato come EUR, invece di far crashare la conversione.
val supportedCartCurrencies = listOf("EUR", "USD")

/**
 * Converte un importo dalla sua valuta originale (quella con cui l'articolo è
 * stato aggiunto al carrello, es. "USD") alla valuta scelta per la
 * visualizzazione (es. "EUR"): a differenza di CatalogPriceFormatter.convert
 * (che assume sempre EUR in partenza), qui serve poter convertire ANCHE da
 * USD verso EUR, perché il carrello può contenere articoli in valute diverse
 * tra loro - sommarli senza convertirli prima darebbe un totale senza senso.
 */
fun convertCartAmount(amount: Double, fromCurrency: String?, toCurrency: String): Double {
    val from = fromCurrency ?: "EUR"
    if (from == toCurrency) return amount

    // Passa sempre per EUR come valuta "ponte", coerente con l'unico tasso
    // disponibile (EUR<->USD): se in futuro si aggiungono altre valute basta
    // aggiungere il loro tasso verso EUR qui, senza toccare i chiamanti.
    val amountInEur = if (from == "USD") amount / EUR_TO_USD_RATE else amount
    return if (toCurrency == "USD") amountInEur * EUR_TO_USD_RATE else amountInEur
}

fun currencySymbol(currency: String) = if (currency == "USD") "$" else "€"
