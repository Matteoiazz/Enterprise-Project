package com.tripify.tripify_android.booking.model

import com.tripify.tripify_android.data.model.BookingLineDTO
import com.tripify.tripify_android.data.model.PassengerResponseDTO

sealed class BoardingPassState {
    object Loading : BoardingPassState()
    data class Success(val lines: List<Pair<BookingLineDTO, List<PassengerResponseDTO>>>) : BoardingPassState()
    data class Error(val message: String) : BoardingPassState()
}
