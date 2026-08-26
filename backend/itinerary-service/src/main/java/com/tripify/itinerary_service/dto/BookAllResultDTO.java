package com.tripify.itinerary_service.dto;

import java.util.List;

public record BookAllResultDTO(int successCount, int total, List<String> errors) {}
