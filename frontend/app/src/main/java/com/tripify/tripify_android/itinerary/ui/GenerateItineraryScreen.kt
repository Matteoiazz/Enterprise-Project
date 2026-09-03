package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.components.CityAutocompleteField
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel
import kotlinx.coroutines.launch

private const val MIN_DAYS = 1
private const val MAX_DAYS = 14
private const val MIN_TRAVELERS = 1
private const val MAX_TRAVELERS = 20

/**
 * Form per generare una bozza di itinerario (volo + hotel + attività scelti dal
 * catalogo esistente): l'utente indica solo città, durata ed eventualmente un budget,
 * poi atterra sul dettaglio della lista già pronta, modificabile come una qualsiasi
 * lista costruita a mano (vedi ItineraryService.generateItinerary).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateItineraryScreen(
    viewModel: ItineraryViewModel,
    catalogViewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onGenerated: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var departureCity by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var days by remember { mutableIntStateOf(3) }
    var travelers by remember { mutableIntStateOf(1) }
    var wantReturnFlight by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CatalogColors.Accent,
        unfocusedBorderColor = CatalogColors.Hairline,
        focusedContainerColor = CatalogColors.Surface,
        unfocusedContainerColor = CatalogColors.Surface,
        cursorColor = CatalogColors.AccentDark,
        focusedTextColor = CatalogColors.Ink,
        unfocusedTextColor = CatalogColors.Ink
    )

    fun submit() {
        if (departureCity.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Indica una città di partenza") }
            return
        }
        if (city.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Indica una città di destinazione") }
            return
        }
        // Solo cifre intere (niente decimali): un budget non ha bisogno di centesimi,
        // ed evita ogni ambiguità tra "." come separatore delle migliaia (uso comune
        // in italiano, es. "1.500") e come separatore decimale.
        val budget = budgetText.trim().toBigDecimalOrNull()
        if (budgetText.isNotBlank() && budget == null) {
            scope.launch { snackbarHostState.showSnackbar("Budget non valido") }
            return
        }
        isGenerating = true
        viewModel.generateItinerary(departureCity.trim(), city.trim(), days, travelers, wantReturnFlight, budget) { newListId, error ->
            isGenerating = false
            if (newListId != null) {
                onGenerated(newListId)
            } else {
                scope.launch { snackbarHostState.showSnackbar(error ?: "Impossibile generare l'itinerario") }
            }
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Genera itinerario", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isGenerating) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = if (isGenerating) CatalogColors.InkSubtle else CatalogColors.Ink
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CatalogSpacing.Gutter, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CatalogShapes.Badge).background(CatalogColors.AccentSoft, CatalogShapes.Badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Dove vuoi andare?", style = CatalogType.Section, color = CatalogColors.Ink)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Scegliamo per te un volo, un hotel e qualche attività dal catalogo. Potrai modificare tutto dopo.",
                style = CatalogType.Body, color = CatalogColors.InkMuted
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text("CITTÀ DI PARTENZA", style = CatalogType.Overline, color = CatalogColors.InkMuted)
            Spacer(modifier = Modifier.height(8.dp))
            CityAutocompleteField(
                value = departureCity,
                onValueChange = { departureCity = it },
                label = "es. Milano",
                icon = Icons.Filled.LocationOn,
                fetchSuggestions = { query -> catalogViewModel.fetchCitySuggestions(query) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("CITTÀ DI DESTINAZIONE", style = CatalogType.Overline, color = CatalogColors.InkMuted)
            Spacer(modifier = Modifier.height(8.dp))
            CityAutocompleteField(
                value = city,
                onValueChange = { city = it },
                label = "es. Roma",
                icon = Icons.Filled.LocationOn,
                fetchSuggestions = { query -> catalogViewModel.fetchCitySuggestions(query) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("DURATA", style = CatalogType.Overline, color = CatalogColors.InkMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(CatalogColors.Surface, CatalogShapes.Field)
                    .border(1.dp, CatalogColors.Hairline, CatalogShapes.Field)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val canDecrease = !isGenerating && days > MIN_DAYS
                val canIncrease = !isGenerating && days < MAX_DAYS
                IconButton(onClick = { if (days > MIN_DAYS) days-- }, enabled = canDecrease) {
                    Icon(Icons.Filled.Remove, contentDescription = "Riduci durata", tint = if (canDecrease) CatalogColors.AccentDark else CatalogColors.InkSubtle)
                }
                Text(
                    if (days == 1) "1 giorno" else "$days giorni",
                    style = CatalogType.BodyStrong, color = CatalogColors.Ink
                )
                IconButton(onClick = { if (days < MAX_DAYS) days++ }, enabled = canIncrease) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumenta durata", tint = if (canIncrease) CatalogColors.AccentDark else CatalogColors.InkSubtle)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("VIAGGIATORI", style = CatalogType.Overline, color = CatalogColors.InkMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(CatalogColors.Surface, CatalogShapes.Field)
                    .border(1.dp, CatalogColors.Hairline, CatalogShapes.Field)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val canDecreaseTravelers = !isGenerating && travelers > MIN_TRAVELERS
                val canIncreaseTravelers = !isGenerating && travelers < MAX_TRAVELERS
                IconButton(onClick = { if (travelers > MIN_TRAVELERS) travelers-- }, enabled = canDecreaseTravelers) {
                    Icon(Icons.Filled.Remove, contentDescription = "Riduci viaggiatori", tint = if (canDecreaseTravelers) CatalogColors.AccentDark else CatalogColors.InkSubtle)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (travelers == 1) "1 viaggiatore" else "$travelers viaggiatori",
                        style = CatalogType.BodyStrong, color = CatalogColors.Ink
                    )
                }
                IconButton(onClick = { if (travelers < MAX_TRAVELERS) travelers++ }, enabled = canIncreaseTravelers) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumenta viaggiatori", tint = if (canIncreaseTravelers) CatalogColors.AccentDark else CatalogColors.InkSubtle)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.height(24.dp))

            val returnFlightBgColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (wantReturnFlight) CatalogColors.AccentSoft else CatalogColors.Surface,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "returnFlightBg"
            )

            val returnFlightBorderColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (wantReturnFlight) CatalogColors.Accent else CatalogColors.Hairline,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "returnFlightBorder"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CatalogShapes.Field)
                    .background(returnFlightBgColor)
                    .border(1.dp, returnFlightBorderColor, CatalogShapes.Field)
                    .clickable(enabled = !isGenerating) { wantReturnFlight = !wantReturnFlight }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (wantReturnFlight) CatalogColors.Accent.copy(alpha = 0.15f) else CatalogColors.SurfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlightLand,
                            contentDescription = null,
                            tint = if (wantReturnFlight) CatalogColors.AccentDark else CatalogColors.InkSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        Text(
                            text = "Volo di ritorno",
                            style = CatalogType.BodyStrong,
                            color = if (wantReturnFlight) CatalogColors.AccentDark else CatalogColors.Ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Aggiungilo automaticamente se disponibile nel catalogo",
                            style = CatalogType.Caption,
                            color = if (wantReturnFlight) CatalogColors.Accent else CatalogColors.InkMuted
                        )
                    }
                }

                Switch(
                    checked = wantReturnFlight,
                    onCheckedChange = null,
                    enabled = !isGenerating,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CatalogColors.AccentDark,
                        uncheckedThumbColor = CatalogColors.Surface,
                        uncheckedTrackColor = CatalogColors.Hairline,
                        uncheckedBorderColor = CatalogColors.Hairline
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("BUDGET (OPZIONALE)", style = CatalogType.Overline, color = CatalogColors.InkMuted)

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { submit() },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                shape = CatalogShapes.Field,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERA ITINERARIO", style = CatalogType.Button, color = Color.White)
                }
            }
        }
    }
}
