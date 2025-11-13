package com.monopolyInMatlab.monopoly.persistence;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.GamePlayer;

import java.util.List;
import java.util.UUID;

public interface RoomRepository {
    List<GameRoom> getAllRooms();
    GameRoom createRoom(CreateRoomRequest createRoomRequest);
    void joinRoom(UUID roomId, GamePlayer player);
    GameRoom findGameRoomById(UUID roomId);
    void saveGameRoom(GameRoom gameRoom);
    void deleteRoom(UUID roomId);
}
