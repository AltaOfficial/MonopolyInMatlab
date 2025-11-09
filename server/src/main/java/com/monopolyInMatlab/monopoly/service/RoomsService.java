package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.Player;
import com.monopolyInMatlab.monopoly.domain.Room;

import java.util.List;

public interface RoomsService {

    Room createRoom(CreateRoomRequest createRoomRequest);

    public void joinRoom(Player player);

    public void leaveRoom();

    public List<Room> getAllRooms();
}
