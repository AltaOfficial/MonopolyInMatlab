package com.monopolyInMatlab.monopoly.presentation;

import com.monopolyInMatlab.monopoly.domain.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameWebsocketController {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/room/{roomId}")
    public void test(@DestinationVariable String roomId, Player player) {

    }
}
