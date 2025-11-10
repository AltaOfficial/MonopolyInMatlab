package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UtilitySpace extends BoardSpace {
    private final int purchasePrice;
    private final int mortgageValue;

    // Mutable state
    private UUID ownerId;
    private boolean isMortgaged;

    public int getCurrentRent(int diceRoll, int utilitiesOwnedByOwner) {
        if (isMortgaged) {
            return 0;
        }

        int multiplier = utilitiesOwnedByOwner == 2 ? 10 : 4;
        return diceRoll * multiplier;
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
