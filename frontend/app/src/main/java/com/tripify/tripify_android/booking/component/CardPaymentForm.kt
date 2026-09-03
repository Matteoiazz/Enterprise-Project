package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.PaymentMethodDto
import java.time.YearMonth

// Stessa lista di Impostazioni > Metodi di pagamento: il circuito si sceglie
// da un menu anche qui, non si indovina più dal numero carta, così il dato
// salvato in Impostazioni è coerente con quello scelto durante il pagamento.
val cardProviderOptions = listOf("Visa", "Mastercard", "American Express", "Maestro")

// Raggruppa il numero carta a blocchi di 4 solo per la visualizzazione: il valore
// memorizzato resta la sequenza di sole cifre inviata al backend.
class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = digits.chunked(4).joinToString(" ")

        // Niente formula arbitraria: chunked(4) non aggiunge uno spazio finale
        // quando la lunghezza è multipla di 4 (es. 4, 8, 12, 16 cifre), quindi
        // una formula fissa "offset + offset/4" può restituire una posizione
        // oltre la fine del testo formattato in quei casi - Compose la considera
        // un mapping invalido e l'app crasha. Contare gli spazi realmente
        // presenti è sempre corretto, qualunque sia la lunghezza.
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val groupsBefore = (offset - 1) / 4
                return (offset + groupsBefore).coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val spacesBefore = formatted.take(offset).count { it == ' ' }
                return (offset - spacesBefore).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// Stato del form di pagamento (metodo salvato scelto, oppure dati di una carta
// nuova) per una singola sessione: riusato sia da CheckoutScreen (pagamento
// subito dopo il checkout) sia da RetryPaymentScreen (pagamento di una
// prenotazione PENDING già esistente), che condividono esattamente le stesse
// regole di validazione.
class CardPaymentFormState {
    // null = "nuova carta" selezionata (o nessun metodo salvato ancora scelto);
    // altrimenti è l'id del metodo salvato scelto dall'utente.
    var selectedSavedMethodId by mutableStateOf<String?>(null)
    var hasAutoSelected by mutableStateOf(false)

    var cardProvider by mutableStateOf(cardProviderOptions.first())
    var cardNumber by mutableStateOf("")
    var cardholderName by mutableStateOf("")
    var expiry by mutableStateOf("")
    var cvv by mutableStateOf("")
    var saveNewCard by mutableStateOf(false)

    // Mese 1-12, e l'anno corrente è accettato solo se il mese è quello
    // corrente o successivo (YearMonth.isBefore confronta mese+anno insieme,
    // non l'anno da solo). Stessa validazione usata in Impostazioni > Metodi
    // di pagamento.
    private val expiryMonth: Int? get() = expiry.take(2).toIntOrNull()
    private val expiryYear: Int? get() = expiry.drop(2).toIntOrNull()
    val expiryValid: Boolean
        get() {
            val month = expiryMonth
            val year = expiryYear
            return expiry.length == 4 && month != null && month in 1..12 &&
                year != null && !YearMonth.of(2000 + year, month).isBefore(YearMonth.now())
        }

    val newCardValid: Boolean
        get() = cardNumber.length == 16 && cardholderName.isNotBlank() && expiryValid && cvv.length == 3

    val isValid: Boolean
        get() = if (selectedSavedMethodId != null) true else newCardValid

    fun cardholderNameError(submitAttempted: Boolean): String? =
        if (submitAttempted && cardholderName.isBlank()) "L'intestatario della carta è obbligatorio" else null

    fun cardNumberError(submitAttempted: Boolean): String? = when {
        cardNumber.isBlank() -> if (submitAttempted) "Il numero della carta è obbligatorio" else null
        cardNumber.length != 16 -> "Il numero della carta deve avere 16 cifre"
        else -> null
    }

    fun expiryError(submitAttempted: Boolean): String? = when {
        expiry.isBlank() -> if (submitAttempted) "La data di scadenza è obbligatoria" else null
        expiry.length < 4 -> null
        expiryMonth == null || expiryMonth !in 1..12 -> "Mese non valido"
        !expiryValid -> "La carta è scaduta"
        else -> null
    }

    fun cvvError(submitAttempted: Boolean): String? = when {
        cvv.isBlank() -> if (submitAttempted) "Il CVV è obbligatorio" else null
        cvv.length != 3 -> "Il CVV deve avere 3 cifre"
        else -> null
    }

    // "MM/AA" per PaymentMethodDto/expirationMonthYear quando si salva la carta.
    fun expirationMonthYear() = "${expiry.take(2)}/${expiry.drop(2)}"
}

// Elenco dei metodi salvati + opzione "nuova carta" con relativo form: identico
// tra CheckoutScreen e RetryPaymentScreen. Al primo arrivo di savedMethods (se
// l'utente non ha ancora scelto nulla) preseleziona il primo metodo salvato,
// invece di partire sempre dal form manuale.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSection(
    state: CardPaymentFormState,
    savedMethods: List<PaymentMethodDto>,
    submitAttempted: Boolean
) {
    LaunchedEffect(savedMethods) {
        if (!state.hasAutoSelected && savedMethods.isNotEmpty()) {
            state.selectedSavedMethodId = savedMethods.first().id
            state.hasAutoSelected = true
        }
    }

    savedMethods.forEach { method ->
        SavedPaymentMethodRow(
            method = method,
            selected = state.selectedSavedMethodId == method.id,
            onClick = { state.selectedSavedMethodId = method.id }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    NewCardOptionRow(
        selected = state.selectedSavedMethodId == null,
        onClick = { state.selectedSavedMethodId = null }
    )

    if (state.selectedSavedMethodId == null) {
        Spacer(modifier = Modifier.height(16.dp))

        var providerExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = !providerExpanded },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = state.cardProvider,
                onValueChange = {},
                readOnly = true,
                label = { Text("Circuito") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                singleLine = true,
                shape = CatalogShapes.Field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatalogColors.Accent,
                    unfocusedBorderColor = CatalogColors.Hairline
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false }
            ) {
                cardProviderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            state.cardProvider = option
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.cardholderName,
            onValueChange = { state.cardholderName = it },
            label = { Text("Intestatario carta") },
            placeholder = { Text("Es. Mario Rossi") },
            isError = state.cardholderNameError(submitAttempted) != null,
            supportingText = state.cardholderNameError(submitAttempted)?.let { { Text(it) } },
            singleLine = true,
            shape = CatalogShapes.Field,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CatalogColors.Accent,
                unfocusedBorderColor = CatalogColors.Hairline
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.cardNumber,
            onValueChange = { value -> state.cardNumber = value.filter { it.isDigit() }.take(16) },
            label = { Text("Numero carta") },
            placeholder = { Text("Es. 4111 1111 1111 1111") },
            leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null, tint = CatalogColors.Accent) },
            isError = state.cardNumberError(submitAttempted) != null,
            supportingText = state.cardNumberError(submitAttempted)?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            singleLine = true,
            shape = CatalogShapes.Field,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CatalogColors.Accent,
                unfocusedBorderColor = CatalogColors.Hairline
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.expiry,
                onValueChange = { value -> state.expiry = value.filter { it.isDigit() }.take(4) },
                label = { Text("Scadenza") },
                placeholder = { Text("MM/AA") },
                isError = state.expiryError(submitAttempted) != null,
                supportingText = state.expiryError(submitAttempted)?.let { { Text(it) } },
                visualTransformation = { text ->
                    val digits = text.text
                    val formatted = if (digits.length > 2) "${digits.take(2)}/${digits.drop(2)}" else digits
                    val offsetMapping = object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int = if (offset > 2) offset + 1 else offset
                        override fun transformedToOriginal(offset: Int): Int = if (offset > 3) offset - 1 else offset.coerceAtMost(2)
                    }
                    TransformedText(AnnotatedString(formatted), offsetMapping)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = CatalogShapes.Field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatalogColors.Accent,
                    unfocusedBorderColor = CatalogColors.Hairline
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.cvv,
                onValueChange = { value -> state.cvv = value.filter { it.isDigit() }.take(3) },
                label = { Text("CVV") },
                placeholder = { Text("123") },
                isError = state.cvvError(submitAttempted) != null,
                supportingText = state.cvvError(submitAttempted)?.let { { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = CatalogShapes.Field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatalogColors.Accent,
                    unfocusedBorderColor = CatalogColors.Hairline
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable { state.saveNewCard = !state.saveNewCard }
        ) {
            Checkbox(
                checked = state.saveNewCard,
                onCheckedChange = { state.saveNewCard = it },
                colors = CheckboxDefaults.colors(checkedColor = CatalogColors.AccentDark)
            )
            Text(
                "Salva questo metodo di pagamento per i prossimi acquisti",
                style = CatalogType.Body,
                color = CatalogColors.InkMuted
            )
        }
    }
}

@Composable
private fun SavedPaymentMethodRow(method: PaymentMethodDto, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CatalogShapes.Field,
        color = if (selected) CatalogColors.AccentSoft else CatalogColors.Surface,
        border = BorderStroke(1.dp, if (selected) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = CatalogColors.Accent)
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column {
                Text(
                    "${method.cardProvider} •••• ${method.lastFourDigits ?: "----"}",
                    style = CatalogType.BodyStrong,
                    color = CatalogColors.Ink
                )
                Text("Scadenza ${method.expirationMonthYear}", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            }
        }
    }
}

@Composable
private fun NewCardOptionRow(selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CatalogShapes.Field,
        color = if (selected) CatalogColors.AccentSoft else CatalogColors.Surface,
        border = BorderStroke(1.dp, if (selected) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = CatalogColors.Accent)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Nuova carta", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
        }
    }
}
