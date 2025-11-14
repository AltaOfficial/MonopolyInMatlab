package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.GamePlayer;

import java.util.List;
import java.util.UUID;

public interface RoomsService {

    GameRoom createRoom(CreateRoomRequest createRoomRequest);

    GamePlayer joinRoom(UUID roomId, GamePlayer player);

    void leaveRoom(UUID roomId, UUID playerId);

    List<GameRoom> getAllRooms();

    GameRoom getGameRoom(UUID roomId);

    void saveGameRoom(GameRoom gameRoom);

    void deleteRoom(UUID roomId);
}
