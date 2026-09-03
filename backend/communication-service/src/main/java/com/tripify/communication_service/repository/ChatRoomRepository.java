package com.tripify.communication_service.repository;

import com.tripify.communication_service.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findByTravelerIdAndHostId(String travelerId, String hostId);

    List<ChatRoom> findByTravelerId(String travelerId);

    List<ChatRoom> findByHostId(String hostId);
}