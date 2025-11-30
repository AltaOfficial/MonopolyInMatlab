package com.monopolyInMatlab.monopoly.service.impl;

import com.monopolyInMatlab.monopoly.config.GameConstants;
import com.monopolyInMatlab.monopoly.domain.*;
import com.monopolyInMatlab.monopoly.dto.TradeOfferDTO;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import com.monopolyInMatlab.monopoly.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private final RoomRepository roomRepository;
    private final Random random = new Random();

    @Override
    public void initializeGame(UUID roomId) {
        GameRoom room = roomRepository.findGameRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        // Initialize game players with starting values
        for (GamePlayer player : room.getGamePlayers()) {
            player.setPosition(GameConstants.STARTING_POSITION);
            player.setMoney(GameConstants.STARTING_MONEY);
        }

        roomRepository.saveGameRoom(room);
    }

    @Override
    public GameRoom startGame(UUID roomId) {
        GameRoom room = getGameRoom(roomId);

        if (room.getGamePhase() != GamePhase.LOBBY) {
            throw new IllegalStateException("Game already started");
        }

        room.setGamePhase(GamePhase.IN_PROGRESS);
        room.setCurrentPlayerIndex(0); // sets the current players turn to be the host

        return room;
    }

    @Override
    public int[] rollDice(UUID roomId, UUID playerId) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);

        if (!room.getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new IllegalStateException("Not your turn");
        }

        int die1 = random.nextInt(6) + 1;
        int die2 = random.nextInt(6) + 1;
        int[] dice = new int[]{die1, die2};
        boolean isDoubles = die1 == die2;

        room.setLastDiceRoll(dice);

        if (player.isInJail()) {
            // Handle jail roll separately
            return dice;
        }

        // Check for doubles
        if (isDoubles) {
            room.setDoublesCount(room.getDoublesCount() + 1);
            if (room.getDoublesCount() >= GameConstants.MAX_DOUBLES_BEFORE_JAIL) {
                player.sendToJail();
                room.setDoublesCount(0);
                return dice;
            }
        }

        // Move player
        int totalRoll = die1 + die2;
        int oldPosition = player.getPosition();
        int newPosition = (oldPosition + totalRoll) % 40;
        player.setPosition(newPosition);

        // Check if passed GO
        if (newPosition < oldPosition || newPosition - oldPosition > 12) {
            player.addMoney(GameConstants.GO_SALARY);
        }

        // Handle landing
        handleLanding(room, player, newPosition);

        return dice;
    }

    private void handleLanding(GameRoom room, GamePlayer player, int position) {
        BoardSpace space = room.getBoardSpaces().get(position);

        switch (space.getSpaceType()) {
            case PROPERTY:
            case RAILROAD:
            case UTILITY:
                handlePropertyLanding(room, player, space);
                break;
            case CHANCE:
                // Card will be drawn via separate endpoint
                break;
            case COMMUNITY_CHEST:
                // Card will be drawn via separate endpoint
                break;
            case TAX:
                handleTaxLanding(room, player, (SpecialSpace) space);
                break;
            case CORNER:
                handleCornerLanding(room, player, position);
                break;
        }
    }

    private void handlePropertyLanding(GameRoom room, GamePlayer player, BoardSpace space) {
        UUID ownerId = null;

        if (space instanceof PropertySpace) {
            ownerId = ((PropertySpace) space).getOwnerId();
        } else if (space instanceof RailroadSpace) {
            ownerId = ((RailroadSpace) space).getOwnerId();
        } else if (space instanceof UtilitySpace) {
            ownerId = ((UtilitySpace) space).getOwnerId();
        }

        if (ownerId == null) {
            // Property is unowned - player can buy
            return;
        }

        if (ownerId.equals(player.getPlayerId())) {
            // Player owns this property - no rent
            return;
        }

        // Calculate and pay rent
        int rent = calculateRent(room, space, room.getLastDiceRoll()[0] + room.getLastDiceRoll()[1], ownerId);
        GamePlayer owner = room.getPlayerById(ownerId);

        if (player.subtractMoney(rent)) {
            owner.addMoney(rent);
        } else {
            // Player can't afford rent - check networth
            handleInsufficientFunds(room, player, rent, owner, "rent payment");
        }
    }

    private int calculateRent(GameRoom room, BoardSpace space, int diceRoll, UUID ownerId) {
        GamePlayer owner = room.getPlayerById(ownerId);

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            boolean hasMonopoly = owner.ownsMonopoly(prop.getColorGroup());
            return prop.getCurrentRent(hasMonopoly);
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            int railroadsOwned = (int) owner.getOwnedPropertyPositions().stream()
                    .map(pos -> room.getBoardSpaces().get(pos))
                    .filter(s -> s instanceof RailroadSpace)
                    .count();
            return rr.getCurrentRent(railroadsOwned);
        } else if (space instanceof UtilitySpace) {
            UtilitySpace util = (UtilitySpace) space;
            int utilitiesOwned = (int) owner.getOwnedPropertyPositions().stream()
                    .map(pos -> room.getBoardSpaces().get(pos))
                    .filter(s -> s instanceof UtilitySpace)
                    .count();
            return util.getCurrentRent(diceRoll, utilitiesOwned);
        }

        return 0;
    }

    private void handleTaxLanding(GameRoom room, GamePlayer player, SpecialSpace space) {
        int taxAmount = space.getTaxAmount();
        if (!player.subtractMoney(taxAmount)) {
            // Player can't afford tax - check networth
            handleInsufficientFunds(room, player, taxAmount, null, "tax payment");
        }
    }

    private void handleCornerLanding(GameRoom room, GamePlayer player, int position) {
        if (position == GameConstants.GO_TO_JAIL_POSITION) {
            player.sendToJail();
        }
        // GO, Jail (just visiting), Free Parking - no action needed
    }

    @Override
    public void buyProperty(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        int price = 0;
        UUID currentOwner = null;
        ColorGroup colorGroup = ColorGroup.NONE;

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            price = prop.getPurchasePrice();
            currentOwner = prop.getOwnerId();
            colorGroup = prop.getColorGroup();
            if (currentOwner == null && player.subtractMoney(price)) {
                prop.setOwnerId(playerId);
                player.addProperty(position, colorGroup);
            } else {
                throw new IllegalStateException("Cannot buy property");
            }
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            price = rr.getPurchasePrice();
            currentOwner = rr.getOwnerId();
            colorGroup = ColorGroup.RAILROAD;
            if (currentOwner == null && player.subtractMoney(price)) {
                rr.setOwnerId(playerId);
                player.addProperty(position, colorGroup);
            } else {
                throw new IllegalStateException("Cannot buy property");
            }
        } else if (space instanceof UtilitySpace util) {
            price = util.getPurchasePrice();
            currentOwner = util.getOwnerId();
            colorGroup = ColorGroup.UTILITY;
            if (currentOwner == null && player.subtractMoney(price)) {
                util.setOwnerId(playerId);
                player.addProperty(position, colorGroup);
            } else {
                throw new IllegalStateException("Cannot buy property");
            }
        }
    }

    @Override
    public void declineProperty(UUID roomId, UUID playerId, int position) {
        // Trigger auction
        startAuction(roomId, position);
    }

    @Override
    public void buildHouse(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        if (!(space instanceof PropertySpace)) {
            throw new IllegalStateException("Can only build on properties");
        }

        PropertySpace prop = (PropertySpace) space;

        if (!prop.getOwnerId().equals(playerId)) {
            throw new IllegalStateException("You don't own this property");
        }

        if (!player.ownsMonopoly(prop.getColorGroup())) {
            throw new IllegalStateException("Must own monopoly to build");
        }

        if (!prop.canBuildHouse()) {
            throw new IllegalStateException("Cannot build house here");
        }

        if (!room.useHouse()) {
            throw new IllegalStateException("No houses available");
        }

        if (player.subtractMoney(prop.getHouseCost())) {
            prop.buildHouse();
            player.setTotalHouses(player.getTotalHouses() + 1);
        } else {
            room.returnHouse();
            throw new IllegalStateException("Cannot afford house");
        }
    }

    @Override
    public void buildHotel(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        if (!(space instanceof PropertySpace)) {
            throw new IllegalStateException("Can only build on properties");
        }

        PropertySpace prop = (PropertySpace) space;

        if (!prop.getOwnerId().equals(playerId)) {
            throw new IllegalStateException("You don't own this property");
        }

        if (!prop.canBuildHotel()) {
            throw new IllegalStateException("Need 4 houses to build hotel");
        }

        if (!room.useHotel()) {
            throw new IllegalStateException("No hotels available");
        }

        if (player.subtractMoney(prop.getHotelCost())) {
            prop.buildHotel();
            // Return 4 houses to bank
            for (int i = 0; i < 4; i++) {
                room.returnHouse();
            }
            player.setTotalHouses(player.getTotalHouses() - 4);
            player.setTotalHotels(player.getTotalHotels() + 1);
        } else {
            room.returnHotel();
            throw new IllegalStateException("Cannot afford hotel");
        }
    }

    @Override
    public void sellHouse(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        if (!(space instanceof PropertySpace)) {
            throw new IllegalStateException("Can only sell houses from properties");
        }

        PropertySpace prop = (PropertySpace) space;

        if (!prop.getOwnerId().equals(playerId)) {
            throw new IllegalStateException("You don't own this property");
        }

        if (prop.getHousesBuilt() == 0) {
            throw new IllegalStateException("No houses to sell on this property");
        }

        // Sell house for 50% of build cost
        int refundAmount = prop.getHouseCost() / 2;
        prop.setHousesBuilt(prop.getHousesBuilt() - 1);
        player.addMoney(refundAmount);
        room.returnHouse();
        player.setTotalHouses(player.getTotalHouses() - 1);
    }

    @Override
    public void sellHotel(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        if (!(space instanceof PropertySpace)) {
            throw new IllegalStateException("Can only sell hotels from properties");
        }

        PropertySpace prop = (PropertySpace) space;

        if (!prop.getOwnerId().equals(playerId)) {
            throw new IllegalStateException("You don't own this property");
        }

        if (!prop.isHasHotel()) {
            throw new IllegalStateException("No hotel to sell on this property");
        }

        // Sell hotel for 50% of build cost
        int refundAmount = prop.getHotelCost() / 2;
        prop.setHasHotel(false);
        prop.setHousesBuilt(0);  // Hotel is removed completely, not downgraded
        player.addMoney(refundAmount);
        room.returnHotel();
        player.setTotalHotels(player.getTotalHotels() - 1);
    }

    @Override
    public void mortgageProperty(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        int mortgageValue = 0;

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            if (prop.getOwnerId().equals(playerId) && prop.canMortgage()) {
                mortgageValue = prop.getMortgageValue();
                prop.mortgage();
                player.addMoney(mortgageValue);
            }
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            if (rr.getOwnerId().equals(playerId) && rr.canMortgage()) {
                mortgageValue = rr.getMortgageValue();
                rr.mortgage();
                player.addMoney(mortgageValue);
            }
        } else if (space instanceof UtilitySpace) {
            UtilitySpace util = (UtilitySpace) space;
            if (util.getOwnerId().equals(playerId) && util.canMortgage()) {
                mortgageValue = util.getMortgageValue();
                util.mortgage();
                player.addMoney(mortgageValue);
            }
        }
    }

    @Override
    public void unmortgageProperty(UUID roomId, UUID playerId, int position) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);
        BoardSpace space = room.getBoardSpaces().get(position);

        int unmortgageCost = 0;

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            if (prop.getOwnerId().equals(playerId) && prop.isMortgaged()) {
                unmortgageCost = (int) (prop.getMortgageValue() * GameConstants.UNMORTGAGE_RATE);
                if (player.subtractMoney(unmortgageCost)) {
                    prop.unmortgage();
                }
            }
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            if (rr.getOwnerId().equals(playerId) && rr.isMortgaged()) {
                unmortgageCost = (int) (rr.getMortgageValue() * GameConstants.UNMORTGAGE_RATE);
                if (player.subtractMoney(unmortgageCost)) {
                    rr.unmortgage();
                }
            }
        } else if (space instanceof UtilitySpace) {
            UtilitySpace util = (UtilitySpace) space;
            if (util.getOwnerId().equals(playerId) && util.isMortgaged()) {
                unmortgageCost = (int) (util.getMortgageValue() * GameConstants.UNMORTGAGE_RATE);
                if (player.subtractMoney(unmortgageCost)) {
                    util.unmortgage();
                }
            }
        }
    }

    @Override
    public void payJailFine(UUID roomId, UUID playerId) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);

        if (!player.isInJail()) {
            throw new IllegalStateException("Player not in jail");
        }

        if (player.subtractMoney(GameConstants.JAIL_FINE)) {
            player.releaseFromJail();
        } else {
            throw new IllegalStateException("Cannot afford jail fine");
        }
    }

    @Override
    public void useGetOutOfJailCard(UUID roomId, UUID playerId) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);

        if (!player.isInJail()) {
            throw new IllegalStateException("Player not in jail");
        }

        if (player.getGetOutOfJailCards() > 0) {
            player.setGetOutOfJailCards(player.getGetOutOfJailCards() - 1);
            player.releaseFromJail();
        } else {
            throw new IllegalStateException("No Get Out of Jail cards");
        }
    }

    @Override
    public boolean rollForJail(UUID roomId, UUID playerId) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);

        if (!player.isInJail()) {
            throw new IllegalStateException("Player not in jail");
        }

        int die1 = random.nextInt(6) + 1;
        int die2 = random.nextInt(6) + 1;
        boolean isDoubles = die1 == die2;

        room.setLastDiceRoll(new int[]{die1, die2});

        if (isDoubles) {
            player.releaseFromJail();
            // Move player
            int totalRoll = die1 + die2;
            player.setPosition((player.getPosition() + totalRoll) % 40);
            return true;
        } else {
            player.setJailTurns(player.getJailTurns() + 1);
            if (player.getJailTurns() >= GameConstants.MAX_JAIL_TURNS) {
                // Must pay fine
                if (player.getMoney() >= GameConstants.JAIL_FINE) {
                    payJailFine(roomId, playerId);
                    return true;
                } else {
                    // Player can't afford jail fine - check networth
                    handleInsufficientFunds(room, player, GameConstants.JAIL_FINE, null, "jail fine");
                }
            }
            return false;
        }
    }

    @Override
    public void proposeTrade(UUID roomId, TradeOfferDTO tradeOffer) {
        GameRoom room = getGameRoom(roomId);

        Trade trade = Trade.builder()
                .tradeId(UUID.randomUUID())
                .fromPlayerId(tradeOffer.getFromPlayerId())
                .toPlayerId(tradeOffer.getToPlayerId())
                .fromPlayerProperties(tradeOffer.getFromPlayerProperties())
                .fromPlayerMoney(tradeOffer.getFromPlayerMoney())
                .toPlayerProperties(tradeOffer.getToPlayerProperties())
                .toPlayerMoney(tradeOffer.getToPlayerMoney())
                .build();

        room.setCurrentTrade(trade);
    }

    @Override
    public void respondToTrade(UUID roomId, UUID playerId, UUID tradeId, boolean accept) {
        GameRoom room = getGameRoom(roomId);
        Trade trade = room.getCurrentTrade();

        if (trade == null || !trade.getTradeId().equals(tradeId)) {
            throw new IllegalStateException("Trade not found");
        }

        if (!trade.getToPlayerId().equals(playerId)) {
            throw new IllegalStateException("Not your trade");
        }

        if (accept) {
            executeTrade(room, trade);
            trade.setStatus(Trade.TradeStatus.ACCEPTED);
        } else {
            trade.setStatus(Trade.TradeStatus.DECLINED);
        }

        room.setCurrentTrade(null);
    }

    private void executeTrade(GameRoom room, Trade trade) {
        GamePlayer fromPlayer = room.getPlayerById(trade.getFromPlayerId());
        GamePlayer toPlayer = room.getPlayerById(trade.getToPlayerId());

        // Transfer properties from fromPlayer to toPlayer
        for (int position : trade.getFromPlayerProperties()) {
            transferProperty(room, fromPlayer, toPlayer, position);
        }

        // Transfer properties from toPlayer to fromPlayer
        for (int position : trade.getToPlayerProperties()) {
            transferProperty(room, toPlayer, fromPlayer, position);
        }

        // Transfer money
        fromPlayer.subtractMoney(trade.getFromPlayerMoney());
        toPlayer.addMoney(trade.getFromPlayerMoney());

        toPlayer.subtractMoney(trade.getToPlayerMoney());
        fromPlayer.addMoney(trade.getToPlayerMoney());
    }

    private void transferProperty(GameRoom room, GamePlayer from, GamePlayer to, int position) {
        BoardSpace space = room.getBoardSpaces().get(position);
        ColorGroup colorGroup = ColorGroup.NONE;

        if (space instanceof PropertySpace) {
            PropertySpace prop = (PropertySpace) space;
            colorGroup = prop.getColorGroup();
            prop.setOwnerId(to.getPlayerId());
        } else if (space instanceof RailroadSpace) {
            RailroadSpace rr = (RailroadSpace) space;
            colorGroup = ColorGroup.RAILROAD;
            rr.setOwnerId(to.getPlayerId());
        } else if (space instanceof UtilitySpace) {
            UtilitySpace util = (UtilitySpace) space;
            colorGroup = ColorGroup.UTILITY;
            util.setOwnerId(to.getPlayerId());
        }

        from.removeProperty(position, colorGroup);
        to.addProperty(position, colorGroup);
    }

    @Override
    public void startAuction(UUID roomId, int propertyPosition) {
        GameRoom room = getGameRoom(roomId);

        Auction auction = Auction.builder()
                .propertyPosition(propertyPosition)
                .highestBid(0)
                .build();

        room.setCurrentAuction(auction);
    }

    @Override
    public void placeBid(UUID roomId, UUID playerId, int amount) {
        GameRoom room = getGameRoom(roomId);
        Auction auction = room.getCurrentAuction();

        if (auction == null || !auction.isActive()) {
            throw new IllegalStateException("No active auction");
        }

        GamePlayer player = room.getPlayerById(playerId);
        if (player.getMoney() < amount) {
            throw new IllegalStateException("Cannot afford bid");
        }

        auction.placeBid(playerId, amount);
    }

    @Override
    public void endAuction(UUID roomId) {
        GameRoom room = getGameRoom(roomId);
        Auction auction = room.getCurrentAuction();

        if (auction == null) {
            return;
        }

        auction.end();

        if (auction.getHighestBidderId() != null) {
            // Award property to highest bidder
            buyProperty(roomId, auction.getHighestBidderId(), auction.getPropertyPosition());
        }

        room.setCurrentAuction(null);
    }

    @Override
    public Card drawCard(UUID roomId, UUID playerId, String deckType) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);

        Card card = deckType.equals("CHANCE")
                ? room.drawChanceCard()
                : room.drawCommunityChestCard();

        executeCardAction(room, player, card);

        return card;
    }

    private void executeCardAction(GameRoom room, GamePlayer player, Card card) {
        switch (card.getActionType()) {
            case ADVANCE_TO_GO:
                player.setPosition(0);
                player.addMoney(GameConstants.GO_SALARY);
                break;
            case ADVANCE_TO_SPACE:
                int targetPosition = card.getValue();
                if (targetPosition == -1) {
                    // Nearest utility
                    targetPosition = findNearestUtility(player.getPosition());
                } else if (targetPosition == -2) {
                    // Nearest railroad
                    targetPosition = findNearestRailroad(player.getPosition());
                }
                if (targetPosition < player.getPosition()) {
                    player.addMoney(GameConstants.GO_SALARY);
                }
                player.setPosition(targetPosition);
                handleLanding(room, player, targetPosition);
                break;
            case GO_BACK_SPACES:
                player.setPosition((player.getPosition() - card.getValue() + 40) % 40);
                break;
            case GO_TO_JAIL:
                player.sendToJail();
                break;
            case GET_OUT_OF_JAIL_FREE:
                player.setGetOutOfJailCards(player.getGetOutOfJailCards() + 1);
                break;
            case COLLECT_MONEY:
                player.addMoney(card.getValue());
                break;
            case PAY_MONEY:
                if (!player.subtractMoney(card.getValue())) {
                    handleInsufficientFunds(room, player, card.getValue(), null, "card payment");
                }
                break;
            case PAY_PER_HOUSE_HOTEL:
                int cost = player.getTotalHouses() * card.getValue() +
                          player.getTotalHotels() * (card.getValue() * 4);
                if (!player.subtractMoney(cost)) {
                    handleInsufficientFunds(room, player, cost, null, "property repairs");
                }
                break;
            case COLLECT_FROM_PLAYERS:
                for (GamePlayer other : room.getGamePlayers()) {
                    if (!other.getPlayerId().equals(player.getPlayerId()) && !other.isBankrupt()) {
                        if (other.subtractMoney(card.getValue())) {
                            player.addMoney(card.getValue());
                        }
                    }
                }
                break;
            case PAY_TO_PLAYERS:
                for (GamePlayer other : room.getGamePlayers()) {
                    if (!other.getPlayerId().equals(player.getPlayerId()) && !other.isBankrupt()) {
                        if (player.subtractMoney(card.getValue())) {
                            other.addMoney(card.getValue());
                        }
                    }
                }
                break;
        }
    }

    private int findNearestUtility(int currentPosition) {
        // Utilities are at positions 12 and 28
        if (currentPosition < 12) return 12;
        if (currentPosition < 28) return 28;
        return 12; // Wrap around
    }

    private int findNearestRailroad(int currentPosition) {
        // Railroads are at 5, 15, 25, 35
        int[] railroads = {5, 15, 25, 35};
        for (int rr : railroads) {
            if (currentPosition < rr) return rr;
        }
        return 5; // Wrap around
    }

    @Override
    public void endTurn(UUID roomId, UUID playerId) {
        GameRoom room = getGameRoom(roomId);

        if (!room.getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new IllegalStateException("Not your turn");
        }

        // Check if player rolled doubles
        if (room.getDoublesCount() > 0 &&
            room.getLastDiceRoll()[0] == room.getLastDiceRoll()[1]) {
            // Player can roll again
            return;
        }

        room.advanceToNextPlayer();

        // Check game over
        if (room.countActivePlayers() == 1) {
            GamePlayer winner = room.getGamePlayers().stream()
                    .filter(p -> !p.isBankrupt())
                    .findFirst()
                    .orElse(null);
            if (winner != null) {
                room.setWinnerId(winner.getPlayerId());
                room.setGamePhase(GamePhase.FINISHED);
            }
        }
    }

    private void handleBankruptcy(GameRoom room, GamePlayer player, GamePlayer creditor) {
        player.declareBankruptcy();

        // Transfer all properties to creditor (or bank if null)
        for (int position : new ArrayList<>(player.getOwnedPropertyPositions())) {
            if (creditor != null) {
                transferProperty(room, player, creditor, position);
            } else {
                // Return to bank - reset ownership
                BoardSpace space = room.getBoardSpaces().get(position);
                if (space instanceof PropertySpace) {
                    ((PropertySpace) space).setOwnerId(null);
                } else if (space instanceof RailroadSpace) {
                    ((RailroadSpace) space).setOwnerId(null);
                } else if (space instanceof UtilitySpace) {
                    ((UtilitySpace) space).setOwnerId(null);
                }
            }
        }

        if (creditor != null) {
            creditor.addMoney(player.getMoney());
        }

        // Check game over
        if (room.countActivePlayers() == 1) {
            GamePlayer winner = room.getGamePlayers().stream()
                    .filter(p -> !p.isBankrupt())
                    .findFirst()
                    .orElse(null);
            if (winner != null) {
                room.setWinnerId(winner.getPlayerId());
                room.setGamePhase(GamePhase.FINISHED);
            }
        }
    }

    @Override
    public GameRoom getGameRoom(UUID roomId) {
        return (GameRoom) roomRepository.getAllRooms().stream()
                .filter(r -> r.getRoomId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    @Override
    public void handlePlayerDisconnect(UUID roomId, UUID playerId) {
        GameRoom room = getGameRoom(roomId);
        room.setGamePhase(GamePhase.FINISHED);
        // Game ends on disconnect as per requirements
    }

    @Override
    public void payOffDebt(UUID roomId, UUID playerId, List<Integer> housesToSell,
                           List<Integer> hotelsToSell, List<Integer> propertiesToMortgage,
                           UUID creditorId, int amountOwed) {
        GameRoom room = getGameRoom(roomId);
        GamePlayer player = room.getPlayerById(playerId);

        // Sell hotels first
        if (hotelsToSell != null) {
            for (Integer position : hotelsToSell) {
                BoardSpace space = room.getBoardSpaces().get(position);
                if (space instanceof PropertySpace) {
                    PropertySpace prop = (PropertySpace) space;
                    if (prop.getOwnerId().equals(playerId) && prop.isHasHotel()) {
                        sellHotel(roomId, playerId, position);
                    }
                }
            }
        }

        // Sell houses
        if (housesToSell != null) {
            for (Integer position : housesToSell) {
                BoardSpace space = room.getBoardSpaces().get(position);
                if (space instanceof PropertySpace) {
                    PropertySpace prop = (PropertySpace) space;
                    if (prop.getOwnerId().equals(playerId) && prop.getHousesBuilt() > 0) {
                        sellHouse(roomId, playerId, position);
                    }
                }
            }
        }

        // Mortgage properties (only if they have no houses/hotels)
        if (propertiesToMortgage != null) {
            for (Integer position : propertiesToMortgage) {
                BoardSpace space = room.getBoardSpaces().get(position);

                // Validate no houses/hotels before mortgaging
                if (space instanceof PropertySpace) {
                    PropertySpace prop = (PropertySpace) space;
                    if (prop.getHousesBuilt() > 0 || prop.isHasHotel()) {
                        throw new IllegalStateException("Cannot mortgage property with buildings");
                    }
                    if (prop.getOwnerId().equals(playerId) && prop.canMortgage()) {
                        mortgageProperty(roomId, playerId, position);
                    }
                } else if (space instanceof RailroadSpace) {
                    RailroadSpace rr = (RailroadSpace) space;
                    if (rr.getOwnerId().equals(playerId) && rr.canMortgage()) {
                        mortgageProperty(roomId, playerId, position);
                    }
                } else if (space instanceof UtilitySpace) {
                    UtilitySpace util = (UtilitySpace) space;
                    if (util.getOwnerId().equals(playerId) && util.canMortgage()) {
                        mortgageProperty(roomId, playerId, position);
                    }
                }
            }
        }

        // Check if enough money was raised
        if (player.getMoney() < amountOwed) {
            // Still can't pay after liquidation - declare bankruptcy
            GamePlayer creditor = creditorId != null ? room.getPlayerById(creditorId) : null;
            handleBankruptcy(room, player, creditor);
            return;
        }

        // Pay the debt
        if (!player.subtractMoney(amountOwed)) {
            // This shouldn't happen, but handle it anyway
            GamePlayer creditor = creditorId != null ? room.getPlayerById(creditorId) : null;
            handleBankruptcy(room, player, creditor);
            return;
        }

        // Transfer money to creditor or bank
        if (creditorId != null) {
            GamePlayer creditor = room.getPlayerById(creditorId);
            creditor.addMoney(amountOwed);
        }
        // If creditor is null, money goes to bank (just removed from player)
    }

    /**
     * Handles insufficient funds by checking networth and either triggering liquidation or bankruptcy
     */
    public void handleInsufficientFunds(GameRoom room, GamePlayer player, int amountOwed,
                                        GamePlayer creditor, String reason) {
        // Calculate player's total networth
        int networth = player.calculateNetWorth(room.getBoardSpaces());

        if (networth < amountOwed) {
            // Player cannot afford even with liquidation - immediate bankruptcy
            handleBankruptcy(room, player, creditor);
        } else {
            // Player can afford with liquidation - trigger liquidation phase
            // This will be handled by the controller to send LIQUIDATION_REQUIRED event
            room.setPendingDebt(player.getPlayerId(), amountOwed,
                               creditor != null ? creditor.getPlayerId() : null, reason);
        }
    }
}
