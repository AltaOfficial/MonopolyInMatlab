package com.monopolyInMatlab.monopoly.persistence.inMemory;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRoomRepository implements RoomRepository {
    private final ConcurrentMap<UUID, GameRoom> gameRooms = new ConcurrentHashMap<>();

    @Override
    public List<Room> getAllRooms() {
        return new ArrayList<>(gameRooms.values());
    }

    @Override
    public Room createRoom(CreateRoomRequest createRoomRequest) {
        GameRoom newRoom = GameRoom.builder()
                .roomId(UUID.randomUUID())
                .roomName(createRoomRequest.getRoomName())
                .players(new ArrayList<>())
                .isStarted(false)
                .build();
        gameRooms.put(newRoom.getRoomId(), newRoom);
        return newRoom;
    }

    @Override
    public void joinRoom(UUID roomId, Player player) {
        GameRoom room = gameRooms.get(roomId);
        if (room != null && !room.isStarted()) {
            room.getPlayers().add(player);
        }
    }

    @Override
    public GameRoom findGameRoomById(UUID roomId) {
        return gameRooms.get(roomId);
    }

    @Override
    public void saveGameRoom(GameRoom gameRoom) {
        gameRooms.put(gameRoom.getRoomId(), gameRoom);
    }

    @Override
    public void deleteRoom(UUID roomId) {
        gameRooms.remove(roomId);
    }
}
