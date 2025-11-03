package com.monopolyInMatlab.monopoly.presentation;

import com.monopolyInMatlab.monopoly.domain.Room;
import com.monopolyInMatlab.monopoly.service.RoomsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {
    private final RoomsService roomsService;

    @GetMapping("/rooms")
    public List<Room> getRooms() {
        return roomsService.getAllRooms();
    }

    @PostMapping("/createroom")
    public Room createRoom(@Re) {

    }
}
