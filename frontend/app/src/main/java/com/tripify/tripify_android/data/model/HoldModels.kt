package com.tripify.tripify_android.data.model

data class HoldResultDto(val holdId: String, val expiresAt: String)

data class RoomHoldRequest(val checkIn: String, val checkOut: String, val rooms: Int, val userId: String)

data class SeatHoldRequest(val seats: Int, val userId: String)

data class AvailabilityDto(val available: Int)
