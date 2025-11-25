package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.Card;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.dto.TradeOfferDTO;

import java.util.UUID;

public interface GameService {
    // Game lifecycle
    void initializeGame(UUID roomId);
    GameRoom startGame(UUID roomId);

    // Turn actions
    int[] rollDice(UUID roomId, UUID playerId);
    void endTurn(UUID roomId, UUID playerId);

    // Property actions
    void buyProperty(UUID roomId, UUID playerId, int position);
    void declineProperty(UUID roomId, UUID playerId, int position);

    // Building actions
    void buildHouse(UUID roomId, UUID playerId, int position);
    void buildHotel(UUID roomId, UUID playerId, int position);

    // Mortgage actions
    void mortgageProperty(UUID roomId, UUID playerId, int position);
    void unmortgageProperty(UUID roomId, UUID playerId, int position);

    // Jail actions
    void payJailFine(UUID roomId, UUID playerId);
    void useGetOutOfJailCard(UUID roomId, UUID playerId);
    boolean rollForJail(UUID roomId, UUID playerId);

    // Trading
    void proposeTrade(UUID roomId, TradeOfferDTO tradeOffer);
    void respondToTrade(UUID roomId, UUID playerId, UUID tradeId, boolean accept);

    // Auction
    void startAuction(UUID roomId, int propertyPosition);
    void placeBid(UUID roomId, UUID playerId, int amount);
    void endAuction(UUID roomId);

    // Card handling
    Card drawCard(UUID roomId, UUID playerId, String deckType);

    // Game state
    GameRoom getGameRoom(UUID roomId);
    void handlePlayerDisconnect(UUID roomId, UUID playerId);
}
