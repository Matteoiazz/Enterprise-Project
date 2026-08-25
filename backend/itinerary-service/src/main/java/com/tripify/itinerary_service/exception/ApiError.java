package com.tripify.itinerary_service.exception;

public record ApiError(int status, String error, String message) {
}
