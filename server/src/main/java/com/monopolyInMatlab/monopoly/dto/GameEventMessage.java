package com.monopolyInMatlab.monopoly.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GameEventMessage {
    private String messageType;  // GAME_STARTED, DICE_ROLLED, PLAYER_MOVED, etc.
    private Map<String, Object> data;  // Flexible data payload
}
