package com.tripify.tripify_android.booking.util

// Controllo di formato del codice fiscale italiano (16 caratteri, struttura
// standard). Verifica solo la FORMA, non il carattere di controllo né i casi
// di omocodia: intercetta errori di battitura o codici palesemente inventati
// senza implementare l'algoritmo ufficiale di calcolo, sproporzionato per un
// dato che qui serve solo a essere "congelato" sulla prenotazione, non a
// verificare l'identità di chi viaggia.
private val TAX_CODE_PATTERN = Regex("^[A-Z]{6}[0-9]{2}[A-EHLMPR-T][0-9]{2}[A-Z][0-9]{3}[A-Z]$")

fun isTaxCodeFormatValid(taxCode: String): Boolean = TAX_CODE_PATTERN.matches(taxCode)

// Algoritmo ufficiale del carattere di controllo (16° carattere): ogni
// carattere delle prime 15 posizioni vale un numero diverso a seconda che la
// sua posizione (1-indicizzata) sia dispari o pari, la somma mod 26 dà
// l'indice del carattere di controllo atteso. Tabelle e algoritmo sono
// standard (Agenzia delle Entrate) - qui solo il controllo aritmetico, non la
// decodifica del luogo di nascita (richiederebbe la tabella dei codici
// catastali dei comuni, sproporzionata rispetto ai dati raccolti dal form).
private val ODD_POSITION_VALUES = mapOf(
    '0' to 1, '1' to 0, '2' to 5, '3' to 7, '4' to 9,
    '5' to 13, '6' to 15, '7' to 17, '8' to 19, '9' to 21,
    'A' to 1, 'B' to 0, 'C' to 5, 'D' to 7, 'E' to 9,
    'F' to 13, 'G' to 15, 'H' to 17, 'I' to 19, 'J' to 21,
    'K' to 2, 'L' to 4, 'M' to 18, 'N' to 20, 'O' to 11,
    'P' to 3, 'Q' to 6, 'R' to 8, 'S' to 12, 'T' to 14,
    'U' to 16, 'V' to 10, 'W' to 22, 'X' to 25, 'Y' to 24, 'Z' to 23
)

private val EVEN_POSITION_VALUES = mapOf(
    '0' to 0, '1' to 1, '2' to 2, '3' to 3, '4' to 4,
    '5' to 5, '6' to 6, '7' to 7, '8' to 8, '9' to 9,
    'A' to 0, 'B' to 1, 'C' to 2, 'D' to 3, 'E' to 4,
    'F' to 5, 'G' to 6, 'H' to 7, 'I' to 8, 'J' to 9,
    'K' to 10, 'L' to 11, 'M' to 12, 'N' to 13, 'O' to 14,
    'P' to 15, 'Q' to 16, 'R' to 17, 'S' to 18, 'T' to 19,
    'U' to 20, 'V' to 21, 'W' to 22, 'X' to 23, 'Y' to 24, 'Z' to 25
)

fun isTaxCodeChecksumValid(taxCode: String): Boolean {
    if (!isTaxCodeFormatValid(taxCode)) return false
    val sum = taxCode.take(15).withIndex().sumOf { (index, char) ->
        // index pari (0, 2, 4...) è la posizione dispari del CF (1ª, 3ª, 5ª...
        // carattere), essendo index 0-indicizzato mentre le posizioni del CF
        // sono 1-indicizzate.
        if (index % 2 == 0) ODD_POSITION_VALUES.getValue(char) else EVEN_POSITION_VALUES.getValue(char)
    }
    val expectedCheckChar = 'A' + (sum % 26)
    return taxCode[15] == expectedCheckChar
}

// Il numero di documento non ha un formato universale (cambia per tipo di
// documento e paese emittente): unico controllo sensato è una lunghezza
// ragionevole, il resto (solo lettere/cifre) è già filtrato in fase di
// digitazione nei form che lo usano.
fun isDocumentNumberLengthValid(documentNumber: String): Boolean = documentNumber.length in 5..20
