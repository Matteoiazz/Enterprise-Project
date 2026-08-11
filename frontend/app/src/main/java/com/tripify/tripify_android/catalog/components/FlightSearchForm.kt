package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

private val Hairline = Color(0xFFE6E2D8)
private val Ink = Color(0xFF1A1A1A)

@Composable
fun FlightSearchForm(
    departure: String,
    onDepartureChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            OutlinedTextField(
                value = departure, onValueChange = onDepartureChange,
                label = { Text("Partenza", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, unfocusedBorderColor = Hairline, focusedLabelColor = TripifyDarkGreen),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().height(52.dp), singleLine = true, shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = destination, onValueChange = onDestinationChange,
                label = { Text("Destinazione", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.FlightLand, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, unfocusedBorderColor = Hairline, focusedLabelColor = TripifyDarkGreen),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().height(52.dp), singleLine = true, shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { /* TODO: date picker - prossimo step */ },
                    border = BorderStroke(1.dp, Hairline),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Ink, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink)
                }
                OutlinedButton(
                    onClick = { /* TODO: selettore pax - prossimo step */ },
                    border = BorderStroke(1.dp, Hairline),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Ink, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1 Pax", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("CERCA VOLI", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
            }
        }
    }
}