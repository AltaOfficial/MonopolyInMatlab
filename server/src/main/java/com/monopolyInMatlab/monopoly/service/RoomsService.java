package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.Room;

import java.util.List;

public interface RoomsService {
    public void createRoom();

    public void joinRoom();

    public void leaveRoom();

    public List<Room> getAllRooms();
}
