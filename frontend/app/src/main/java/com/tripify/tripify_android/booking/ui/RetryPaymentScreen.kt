package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.tripify.tripify_android.booking.component.CardPaymentFormState
import com.tripify.tripify_android.booking.component.PaymentMethodSection
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.model.BookingLineDTO

// Riprova il pagamento di una prenotazione rimasta PENDING (es. la carta è
// stata rifiutata al primo tentativo dopo il checkout): a differenza di
// CheckoutScreen qui non si raccolgono più gli ospiti (la Booking e le sue
// righe esistono già, gli ospiti si aggiungono con "Aggiungi passeggeri"),
// si paga soltanto. Se un blocco di camera/posto è scaduto nel frattempo il
// backend lo segnala chiaramente invece di confermare qualcosa che potrebbe
// essere stato preso da qualcun altro (vedi BookingService.confirmPayment).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetryPaymentScreen(
    viewModel: BookingViewModel,
    catalogViewModel: CatalogViewModel,
    bookingId: Long,
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val savedMethods by viewModel.savedPaymentMethods.collectAsState()

    val paymentFormState = remember { CardPaymentFormState() }
    var submitAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSavedPaymentMethods()
        viewModel.resetPaymentState()
        if (uiState !is BookingState.Success) {
            viewModel.fetchUserBookings()
        }
    }

    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Success) {
            onPaymentSuccess()
        }
    }

    val booking = (uiState as? BookingState.Success)?.bookings?.find { it.id == bookingId }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Completa il pagamento", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        },
        bottomBar = {
            if (booking != null) {
                Surface(color = CatalogColors.Surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale da pagare", style = CatalogType.Body, color = CatalogColors.InkMuted)
                            Text(
                                text = "€${"%.2f".format(booking.totalAmount)}",
                                style = CatalogType.PriceLarge,
                                color = CatalogColors.AccentDark
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (!paymentFormState.isValid) {
                                    submitAttempted = true
                                } else {
                                    val savedId = paymentFormState.selectedSavedMethodId
                                    if (savedId != null) {
                                        viewModel.retryPaymentWithSavedMethod(booking.id, booking.totalAmount, savedId)
                                    } else {
                                        viewModel.retryPaymentWithNewCard(booking.id, booking.totalAmount, paymentFormState.cardNumber)
                                    }
                                }
                            },
                            enabled = paymentState !is PaymentState.Processing,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Field
                        ) {
                            if (paymentState is PaymentState.Processing) {
                                CircularProgressIndicator(
                                    color = CatalogColors.Surface,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Paga €${"%.2f".format(booking.totalAmount)}", style = CatalogType.Button)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (booking == null) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CatalogColors.AccentDark)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                item {
                    Text(
                        text = "Riepilogo ordine",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                booking.lines.forEach { line ->
                    item(key = "line-${line.id}") {
                        RetryPaymentLineRow(line = line, catalogViewModel = catalogViewModel)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Metodo di pagamento",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    PaymentMethodSection(
                        state = paymentFormState,
                        savedMethods = savedMethods,
                        submitAttempted = submitAttempted
                    )

                    val paymentError = paymentState
                    if (paymentError is PaymentState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = paymentError.message,
                            style = CatalogType.Body,
                            color = CatalogColors.Alert,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Stesso stile di riga foto+nome+prezzo di CartItemCard/BookingCard: qui è di
// sola lettura, la quantità/il prezzo sono già quelli congelati alla Booking.
@Composable
private fun RetryPaymentLineRow(line: BookingLineDTO, catalogViewModel: CatalogViewModel) {
    var resolved by remember(line.catalogItemId) { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(line.catalogItemId) {
        resolved = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = resolved?.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resolved?.title ?: "Articolo #${line.catalogItemId}",
                style = CatalogType.BodyStrong,
                color = CatalogColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Quantità: ${line.quantity ?: 1}",
                style = CatalogType.Caption,
                color = CatalogColors.InkMuted
            )
        }
        Text(
            text = "€${"%.2f".format(line.price)}",
            style = CatalogType.Price,
            color = CatalogColors.AccentDark
        )
    }
}
