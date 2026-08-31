package com.tripify.communication_service.dto;

import lombok.Data;

@Data
public class ChatRoomDto {
    private String id;
    private String travelerId;
    private String hostId;
    private String title;
    private int unreadCount;
}