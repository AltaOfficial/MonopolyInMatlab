package com.monopolyInMatlab.monopoly.persistence;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;

import java.util.List;

public interface RoomRepository {
    List<Room> getAllRooms();
    Room createRoom(CreateRoomRequest createRoomRequest);
    void joinRoom(Player player);
}
