package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RailroadSpace extends BoardSpace {
    private final int purchasePrice;
    private final int mortgageValue;

    // Mutable state
    private UUID ownerId;
    private boolean isMortgaged;

    public int getCurrentRent(int railroadsOwnedByOwner) {
        if (isMortgaged) {
            return 0;
        }

        switch (railroadsOwnedByOwner) {
            case 1: return 25;
            case 2: return 50;
            case 3: return 100;
            case 4: return 200;
            default: return 0;
        }
    }

    public boolean canMortgage() {
        return !isMortgaged;
    }

    public void mortgage() {
        if (canMortgage()) {
            isMortgaged = true;
        }
    }

    public void unmortgage() {
        if (isMortgaged) {
            isMortgaged = false;
        }
    }
}
