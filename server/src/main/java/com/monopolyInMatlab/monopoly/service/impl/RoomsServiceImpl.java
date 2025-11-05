package com.monopolyInMatlab.monopoly.service.impl;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.Room;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import com.monopolyInMatlab.monopoly.service.RoomsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomsServiceImpl implements RoomsService {
    private final RoomRepository roomsRepository;

    @Override
    public Room createRoom(CreateRoomRequest createRoomRequest) {
        return roomsRepository.createRoom(createRoomRequest);
    }

    @Override
    public void joinRoom() {

    }

    @Override
    public void leaveRoom() {

    }

    @Override
    public List<Room> getAllRooms() {
        return roomsRepository.getAllRooms();
    }
}
