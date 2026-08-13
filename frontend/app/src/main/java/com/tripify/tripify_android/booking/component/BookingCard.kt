package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.data.model.BookingResponseDTO

@Composable
fun BookingCard(
    booking: BookingResponseDTO,
    onInviteClick: (Long) -> Unit // Passiamo l'azione da eseguire al click
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Dettagli base del viaggio
            Text(text = "Viaggio #${booking.id}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Data: ${booking.bookingDate}")
            Text(text = "Stato: ${booking.status}")
            Text(text = "Totale: €${booking.totalAmount}")

            Spacer(modifier = Modifier.height(12.dp))

            // ECCO DOVE VA IL TUO CODICE!
            // La logica visiva per capire chi guarda la card
            if (booking.isLeader) {

                // Disegna il bottone per il Leader
                Button(
                    onClick = { onInviteClick(booking.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Invita un amico")
                }

            } else {

                // Disegna il testo per l'ospite
                Text(
                    text = "Sei un partecipante a questo viaggio",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )

            }
        }
    }
}