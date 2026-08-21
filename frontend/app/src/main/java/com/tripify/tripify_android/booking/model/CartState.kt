package com.tripify.tripify_android.booking.model

import com.tripify.tripify_android.data.model.CartDTO

sealed class CartState {
    object Loading : CartState()
    data class Success(val cart: CartDTO) : CartState()
    data class Error(val message: String) : CartState()
}