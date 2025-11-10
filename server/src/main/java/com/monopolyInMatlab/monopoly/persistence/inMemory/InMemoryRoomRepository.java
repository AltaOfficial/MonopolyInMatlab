package com.monopolyInMatlab.monopoly.persistence.inMemory;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRoomRepository implements RoomRepository {
    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public List<Room> getAllRooms() {
        return List.copyOf(rooms.values());
    }

    @Override
    public Room createRoom(CreateRoomRequest createRoomRequest) {
        GameRoom newRoom = GameRoom.builder()
                .roomId(UUID.randomUUID())
                .roomName(createRoomRequest.getRoomName())
                .players(new java.util.LinkedList<>())
                .isStarted(false)
                .build();
        rooms.putIfAbsent(createRoomRequest.getRoomName(), newRoom);
        return newRoom;
    }

    @Override
    public void joinRoom(Player player) {
        // Find room and add player - simplified implementation
        // In production, would need proper room lookup
    }
}
