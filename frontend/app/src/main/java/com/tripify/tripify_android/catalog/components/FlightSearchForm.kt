package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@Composable
fun FlightSearchForm(modifier: Modifier = Modifier) {
    var da by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            OutlinedTextField(
                value = da, onValueChange = { da = it },
                label = { Text("Partenza") },
                leadingIcon = { Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = TripifyGreen) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, focusedLabelColor = TripifyDarkGreen),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = a, onValueChange = { a = it },
                label = { Text("Destinazione") },
                leadingIcon = { Icon(Icons.Filled.FlightLand, contentDescription = null, tint = TripifyGreen) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, focusedLabelColor = TripifyDarkGreen),
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = SfondoPremium, contentColor = TripifyDarkGreen),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date")
                }
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = SfondoPremium, contentColor = TripifyDarkGreen),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1 Pax")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("CERCA VOLI", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}