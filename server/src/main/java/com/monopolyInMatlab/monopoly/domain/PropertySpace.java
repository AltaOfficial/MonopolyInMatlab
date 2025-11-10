package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PropertySpace extends BoardSpace {
    private final ColorGroup colorGroup;
    private final int purchasePrice;
    private final int mortgageValue;
    private final int rentBase;
    private final int rent1House;
    private final int rent2House;
    private final int rent3House;
    private final int rent4House;
    private final int rentHotel;
    private final int houseCost;
    private final int hotelCost;

    // Mutable state
    private UUID ownerId;
    private boolean isMortgaged;
    private int housesBuilt;  // 0-4
    private boolean hasHotel;

    public int getCurrentRent(boolean ownerHasMonopoly) {
        if (isMortgaged) {
            return 0;
        }

        if (hasHotel) {
            return rentHotel;
        }

        switch (housesBuilt) {
            case 1: return rent1House;
            case 2: return rent2House;
            case 3: return rent3House;
            case 4: return rent4House;
            case 0:
            default:
                // Double rent if monopoly with no houses
                return ownerHasMonopoly ? rentBase * 2 : rentBase;
        }
    }

    public boolean canBuildHouse() {
        return !isMortgaged && housesBuilt < 4 && !hasHotel;
    }

    public boolean canBuildHotel() {
        return !isMortgaged && housesBuilt == 4 && !hasHotel;
    }

    public boolean canMortgage() {
        return !isMortgaged && housesBuilt == 0 && !hasHotel;
    }

    public void buildHouse() {
        if (canBuildHouse()) {
            housesBuilt++;
        }
    }

    public void buildHotel() {
        if (canBuildHotel()) {
            housesBuilt = 0;
            hasHotel = true;
        }
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
