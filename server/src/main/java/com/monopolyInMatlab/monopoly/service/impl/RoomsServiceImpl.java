package com.monopolyInMatlab.monopoly.service.impl;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import com.monopolyInMatlab.monopoly.service.RoomsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomsServiceImpl implements RoomsService {
    private final RoomRepository roomsRepository;

    @Override
    public Room createRoom(CreateRoomRequest createRoomRequest) {
        return roomsRepository.createRoom(createRoomRequest);
    }

    @Override
    public void joinRoom(UUID roomId, Player player) {
        roomsRepository.joinRoom(roomId, player);
    }

    @Override
    public void leaveRoom(UUID roomId, UUID playerId) {
        GameRoom room = roomsRepository.findGameRoomById(roomId);
        if (room != null) {
            room.getPlayers().removeIf(player -> player.getPlayerId().equals(playerId));
            roomsRepository.saveGameRoom(room);
        }
    }

    @Override
    public List<Room> getAllRooms() {
        return roomsRepository.getAllRooms();
    }

    @Override
    public GameRoom getGameRoom(UUID roomId) {
        return roomsRepository.findGameRoomById(roomId);
    }

    @Override
    public void saveGameRoom(GameRoom gameRoom) {
        roomsRepository.saveGameRoom(gameRoom);
    }

    @Override
    public void deleteRoom(UUID roomId) {
        roomsRepository.deleteRoom(roomId);
    }
}
