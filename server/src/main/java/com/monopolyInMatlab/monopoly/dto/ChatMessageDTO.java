package com.monopolyInMatlab.monopoly.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ChatMessageDTO {
    private String messageType;  // Always "CHAT_MESSAGE"
    private UUID playerId;
    private String playerName;
    private String message;
    private Instant timestamp;
}
