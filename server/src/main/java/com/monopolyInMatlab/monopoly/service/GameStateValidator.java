package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GameStateValidator {

    public boolean canRollDice(GameRoom room, UUID playerId) {
        if (room.getGamePhase() != GamePhase.IN_PROGRESS) {
            return false;
        }
        return room.getCurrentPlayer().getPlayerId().equals(playerId);
    }

    public boolean canBuyProperty(GameRoom room, UUID playerId, int position) {
        BoardSpace space = room.getBoardSpaces().get(position);
        GamePlayer player = room.getPlayerById(playerId);

        if (player.getPosition() != position) {
            return false;
        }

        int price = 0;
        UUID currentOwner = null;

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            price = prop.getPurchasePrice();
            currentOwner = prop.getOwnerId();
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            price = rr.getPurchasePrice();
            currentOwner = rr.getOwnerId();
        } else if (space instanceof UtilitySpace) {
            UtilitySpace util = (UtilitySpace) space;
            price = util.getPurchasePrice();
            currentOwner = util.getOwnerId();
        } else {
            return false;
        }

        return currentOwner == null && player.getMoney() >= price;
    }

    public boolean canBuildHouse(GameRoom room, UUID playerId, int position) {
        BoardSpace space = room.getBoardSpaces().get(position);
        if (!(space instanceof PropertySpace)) {
            return false;
        }

        PropertySpace prop = (PropertySpace) space;
        GamePlayer player = room.getPlayerById(playerId);

        return prop.getOwnerId() != null &&
               prop.getOwnerId().equals(playerId) &&
               player.ownsMonopoly(prop.getColorGroup()) &&
               prop.canBuildHouse() &&
               player.getMoney() >= prop.getHouseCost() &&
               room.getTotalHousesRemaining() > 0;
    }

    public boolean canBuildHotel(GameRoom room, UUID playerId, int position) {
        BoardSpace space = room.getBoardSpaces().get(position);
        if (!(space instanceof PropertySpace)) {
            return false;
        }

        PropertySpace prop = (PropertySpace) space;
        GamePlayer player = room.getPlayerById(playerId);

        return prop.getOwnerId() != null &&
               prop.getOwnerId().equals(playerId) &&
               prop.canBuildHotel() &&
               player.getMoney() >= prop.getHotelCost() &&
               room.getTotalHotelsRemaining() > 0;
    }

    public boolean canMortgage(GameRoom room, UUID playerId, int position) {
        BoardSpace space = room.getBoardSpaces().get(position);
        GamePlayer player = room.getPlayerById(playerId);

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            return prop.getOwnerId() != null &&
                   prop.getOwnerId().equals(playerId) &&
                   prop.canMortgage();
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            return rr.getOwnerId() != null &&
                   rr.getOwnerId().equals(playerId) &&
                   rr.canMortgage();
        } else if (space instanceof UtilitySpace) {
            UtilitySpace util = (UtilitySpace) space;
            return util.getOwnerId() != null &&
                   util.getOwnerId().equals(playerId) &&
                   util.canMortgage();
        }

        return false;
    }

    public boolean canTrade(GameRoom room, UUID playerId) {
        GamePlayer player = room.getPlayerById(playerId);
        return player != null && !player.isBankrupt();
    }

    public boolean canPlaceBid(GameRoom room, UUID playerId, int amount) {
        Auction auction = room.getCurrentAuction();
        if (auction == null || !auction.isActive()) {
            return false;
        }

        GamePlayer player = room.getPlayerById(playerId);
        return player != null &&
               !player.isBankrupt() &&
               player.getMoney() >= amount &&
               amount > auction.getHighestBid();
    }

    public boolean isPlayerTurn(GameRoom room, UUID playerId) {
        if (room.getCurrentPlayer() == null) {
            return false;
        }
        return room.getCurrentPlayer().getPlayerId().equals(playerId);
    }
}
