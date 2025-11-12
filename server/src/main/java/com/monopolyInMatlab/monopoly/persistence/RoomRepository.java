package com.monopolyInMatlab.monopoly.persistence;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;

import java.util.List;
import java.util.UUID;

public interface RoomRepository {
    List<Room> getAllRooms();
    Room createRoom(CreateRoomRequest createRoomRequest);
    void joinRoom(UUID roomId, Player player);
    GameRoom findGameRoomById(UUID roomId);
    void saveGameRoom(GameRoom gameRoom);
    void deleteRoom(UUID roomId);
}
