package com.monopolyInMatlab.monopoly.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class GameActionRequest {
    private UUID playerId;
    private String playerName;
    private Integer position;  // For property-related actions
    private Integer amount;    // For bids, payments, etc.
    private String action;     // For jail actions: "PAY", "CARD", "ROLL"
}
