package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@Builder
public class GamePlayer {
    // Player fields
    @Builder.Default
    private UUID playerId = UUID.randomUUID();
    private String playerName;

    // Game fields
    @Builder.Default
    private int position = 0;  // 0-39, starts at GO (position 0)

    @Builder.Default
    private int money = 1500;  // Starting money

    @Builder.Default
    private List<Integer> ownedPropertyPositions = new ArrayList<>();

    @Builder.Default
    private boolean inJail = false;

    @Builder.Default
    private int jailTurns = 0;  // Turns spent in jail

    @Builder.Default
    private int getOutOfJailCards = 0;

    @Builder.Default
    private boolean isBankrupt = false;

    @Builder.Default
    private Map<ColorGroup, Integer> colorGroupCounts = new EnumMap<>(ColorGroup.class);

    @Builder.Default
    private int totalHouses = 0;

    @Builder.Default
    private int totalHotels = 0;

    public void addMoney(int amount) {
        this.money += amount;
    }

    public boolean subtractMoney(int amount) {
        if (money >= amount) {
            money -= amount;
            return true;
        }
        return false;
    }

    public void moveToPosition(int newPosition) {
        this.position = newPosition % 40;
    }

    public boolean passedGo(int oldPosition, int newPosition) {
        return newPosition < oldPosition || newPosition - oldPosition > 12;  // Detect wrap-around
    }

    public void sendToJail() {
        this.position = 10;  // Jail position
        this.inJail = true;
        this.jailTurns = 0;
    }

    public void releaseFromJail() {
        this.inJail = false;
        this.jailTurns = 0;
    }

    public void addProperty(int propertyPosition, ColorGroup colorGroup) {
        ownedPropertyPositions.add(propertyPosition);
        colorGroupCounts.put(colorGroup, colorGroupCounts.getOrDefault(colorGroup, 0) + 1);
    }

    public void removeProperty(int propertyPosition, ColorGroup colorGroup) {
        ownedPropertyPositions.remove(Integer.valueOf(propertyPosition));
        int count = colorGroupCounts.getOrDefault(colorGroup, 0);
        if (count > 1) {
            colorGroupCounts.put(colorGroup, count - 1);
        } else {
            colorGroupCounts.remove(colorGroup);
        }
    }

    public boolean ownsMonopoly(ColorGroup colorGroup) {
        int required = getPropertiesInColorGroup(colorGroup);
        return colorGroupCounts.getOrDefault(colorGroup, 0) == required;
    }

    private int getPropertiesInColorGroup(ColorGroup colorGroup) {
        switch (colorGroup) {
            case BROWN:
            case DARK_BLUE:
                return 2;
            case RAILROAD:
            case UTILITY:
                return 4;  // Though these don't form true monopolies
            default:
                return 3;
        }
    }

    public int calculateNetWorth(List<BoardSpace> boardSpaces) {
        int netWorth = money;

        for (int position : ownedPropertyPositions) {
            BoardSpace space = boardSpaces.get(position);
            if (space instanceof PropertySpace) {
                PropertySpace prop = (PropertySpace) space;
                netWorth += prop.isMortgaged() ? 0 : prop.getPurchasePrice();
                netWorth += prop.getHousesBuilt() * prop.getHouseCost();
                if (prop.isHasHotel()) {
                    netWorth += prop.getHotelCost();
                }
            } else if (space instanceof RailroadSpace) {
                RailroadSpace rr = (RailroadSpace) space;
                netWorth += rr.isMortgaged() ? 0 : rr.getPurchasePrice();
            } else if (space instanceof UtilitySpace) {
                UtilitySpace util = (UtilitySpace) space;
                netWorth += util.isMortgaged() ? 0 : util.getPurchasePrice();
            }
        }

        return netWorth;
    }

    public void declareBankruptcy() {
        this.isBankrupt = true;
        this.money = 0;
    }
}
