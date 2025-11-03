package com.monopolyInMatlab.monopoly.persistence;

import com.monopolyInMatlab.monopoly.domain.Room;

import java.util.List;

public interface RoomRepository {
    List<Room> getAllRooms();
}
