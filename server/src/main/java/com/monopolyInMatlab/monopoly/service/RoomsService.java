package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;

import java.util.List;
import java.util.UUID;

public interface RoomsService {

    Room createRoom(CreateRoomRequest createRoomRequest);

    void joinRoom(UUID roomId, Player player);

    void leaveRoom(UUID roomId, UUID playerId);

    List<Room> getAllRooms();

    GameRoom getGameRoom(UUID roomId);

    void saveGameRoom(GameRoom gameRoom);

    void deleteRoom(UUID roomId);
}
