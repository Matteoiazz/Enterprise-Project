package com.tripify.tripify_android

import com.tripify.tripify_android.catalog.ui.HybridHomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent è il punto di ingresso per Jetpack Compose
        setContent {
            // Chiamiamo direttamente la tua schermata ibrida SiVola/Ryanair!
            HybridHomeScreen()
        }
    }
}