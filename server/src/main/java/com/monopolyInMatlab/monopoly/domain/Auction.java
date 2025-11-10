package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Auction {
    private final int propertyPosition;
    private UUID highestBidderId;
    private int highestBid;

    @Builder.Default
    private boolean isActive = true;

    public void placeBid(UUID playerId, int amount) {
        if (isActive && amount > highestBid) {
            highestBidderId = playerId;
            highestBid = amount;
        }
    }

    public void end() {
        isActive = false;
    }
}
