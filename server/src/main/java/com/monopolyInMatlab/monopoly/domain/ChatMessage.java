package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ChatMessage {
    private final UUID playerId;
    private final String playerName;
    private final String message;
}
