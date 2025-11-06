package com.monopolyInMatlab.monopoly.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
public class WebsocketEventListener {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent session) {
        System.out.println("Received a new web socket connection" + session);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent session) {
        System.out.println("Received a new web socket subscription");
    }
}
