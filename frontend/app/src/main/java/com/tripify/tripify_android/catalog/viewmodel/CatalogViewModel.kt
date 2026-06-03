package com.tripify.tripify_android.catalog.viewmodel

import androidx.lifecycle.ViewModel
import com.tripify.tripify_android.catalog.model.CatalogItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CatalogViewModel : ViewModel() {

    // Lo stato della categoria selezionata ("Tutti", "Voli", "Hotel", "Escursioni")
    private val _selectedCategory = MutableStateFlow("Tutti")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // I dati grezzi che in futuro arriveranno dal tuo backend tramite Retrofit
    private val allItems = listOf(
        CatalogItem.Flight(1, "Volo Diretto NY", "€ 450", "https://picsum.photos/seed/ny/600/800", "Fiumicino", "JFK"),
        CatalogItem.Hotel(2, "Napoliviva Bovio", "€ 90/notte", "https://picsum.photos/seed/napoli/600/800", "Via Maio di Porto 9, Napoli", 4.8),
        CatalogItem.Excursion(3, "Tour Stadio Armando Maradona", "€ 35", "https://picsum.photos/seed/stadio/600/800", "3 Ore", true),
        CatalogItem.Flight(4, "Volo per Tokyo", "€ 850", "https://picsum.photos/seed/tokyo/600/800", "Malpensa", "Haneda"),
        CatalogItem.Hotel(5, "Resort Maldive", "€ 300/notte", "https://picsum.photos/seed/maldive/600/800", "Atollo di Male", 5.0)
    )

    // La lista filtrata che la UI andrà a leggere
    private val _catalogList = MutableStateFlow(allItems)
    val catalogList: StateFlow<List<CatalogItem>> = _catalogList.asStateFlow()

    // Funzione per cambiare categoria (come un endpoint che risponde al click)
    fun setCategory(category: String) {
        _selectedCategory.value = category
        _catalogList.value = when (category) {
            "Voli" -> allItems.filterIsInstance<CatalogItem.Flight>()
            "Hotel" -> allItems.filterIsInstance<CatalogItem.Hotel>()
            "Escursioni" -> allItems.filterIsInstance<CatalogItem.Excursion>()
            else -> allItems
        }
    }
}