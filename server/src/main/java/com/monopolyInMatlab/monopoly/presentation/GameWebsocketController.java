package com.monopolyInMatlab.monopoly.presentation;

import com.monopolyInMatlab.monopoly.domain.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameWebsocketController {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, @Payload Player player) {
        System.out.println("Player " + player.getPlayerName() + " joined room " + roomId);
        simpMessagingTemplate.convertAndSend("/room/123", "Player " + player.getPlayerName() + " joined room " + roomId);
    }

}
