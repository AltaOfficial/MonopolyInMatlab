package com.monopolyInMatlab.monopoly.persistence.inMemory;

import com.monopolyInMatlab.monopoly.domain.Room;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRoomRepository implements RoomRepository {
    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public List<Room> getAllRooms() {
        return List.copyOf(rooms.values());
    }
}
