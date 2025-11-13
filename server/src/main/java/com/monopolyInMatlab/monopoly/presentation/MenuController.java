package com.monopolyInMatlab.monopoly.presentation;

import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.GamePlayer;
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
    public List<GameRoom> getRooms() {
        return roomsService.getAllRooms();
    }

    @GetMapping("/room/{roomId}")
    public GameRoom getRoom(@PathVariable("roomId") UUID roomId) {
        return roomsService.getGameRoom(roomId);
    }

    @PostMapping("/createroom")
    public GameRoom createRoom(@RequestBody CreateRoomRequest createRoomRequest) {
        return roomsService.createRoom(createRoomRequest);
    }

    @PostMapping("/rooms/{roomId}/join")
    public void joinRoom(@PathVariable UUID roomId, @RequestBody GamePlayer player) {
        roomsService.joinRoom(roomId, player);
    }

    @PostMapping("/rooms/{roomId}/leave")
    public void leaveRoom(@PathVariable UUID roomId, @RequestBody UUID playerId) {
        roomsService.leaveRoom(roomId, playerId);
    }
}
