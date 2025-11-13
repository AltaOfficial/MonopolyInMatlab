package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@Builder
public class GameRoom {
    // Room fields
    private UUID roomId;
    private String roomName;

    @Builder.Default
    private boolean isStarted = false;

    // Game fields
    @Builder.Default
    private List<BoardSpace> boardSpaces = new ArrayList<>();

    @Builder.Default
    private List<GamePlayer> gamePlayers = new ArrayList<>();

    @Builder.Default
    private List<Card> chanceCards = new ArrayList<>();

    @Builder.Default
    private List<Card> communityChestCards = new ArrayList<>();

    @Builder.Default
    private int chanceCardIndex = 0;

    @Builder.Default
    private int communityChestCardIndex = 0;

    @Builder.Default
    private GamePhase gamePhase = GamePhase.LOBBY;

    @Builder.Default
    private int currentPlayerIndex = 0;

    @Builder.Default
    private int[] lastDiceRoll = new int[]{0, 0};

    @Builder.Default
    private int doublesCount = 0;

    @Builder.Default
    private Auction currentAuction = null;

    @Builder.Default
    private Trade currentTrade = null;

    @Builder.Default
    private List<ChatMessage> chatHistory = new ArrayList<>();

    @Builder.Default
    private UUID winnerId = null;

    @Builder.Default
    private int totalHousesRemaining = 32;

    @Builder.Default
    private int totalHotelsRemaining = 12;

    public GamePlayer getCurrentPlayer() {
        if (gamePlayers.isEmpty()) {
            return null;
        }
        return gamePlayers.get(currentPlayerIndex);
    }

    public void advanceToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % gamePlayers.size();
        } while (gamePlayers.get(currentPlayerIndex).isBankrupt());

        doublesCount = 0;  // Reset doubles count for new player
    }

    public Card drawChanceCard() {
        Card card = chanceCards.get(chanceCardIndex);
        chanceCardIndex = (chanceCardIndex + 1) % chanceCards.size();
        return card;
    }

    public Card drawCommunityChestCard() {
        Card card = communityChestCards.get(communityChestCardIndex);
        communityChestCardIndex = (communityChestCardIndex + 1) % communityChestCards.size();
        return card;
    }

    public void shuffleChanceCards() {
        Collections.shuffle(chanceCards);
        chanceCardIndex = 0;
    }

    public void shuffleCommunityChestCards() {
        Collections.shuffle(communityChestCards);
        communityChestCardIndex = 0;
    }

    public void addChatMessage(ChatMessage message) {
        chatHistory.add(message);
    }

    public GamePlayer getPlayerById(UUID playerId) {
        return gamePlayers.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public int countActivePlayers() {
        return (int) gamePlayers.stream()
                .filter(p -> !p.isBankrupt())
                .count();
    }

    public GamePlayer getWinner() {
        if (winnerId != null) {
            return getPlayerById(winnerId);
        }
        return null;
    }

    public boolean useHouse() {
        if (totalHousesRemaining > 0) {
            totalHousesRemaining--;
            return true;
        }
        return false;
    }

    public void returnHouse() {
        if (totalHousesRemaining < 32) {
            totalHousesRemaining++;
        }
    }

    public boolean useHotel() {
        if (totalHotelsRemaining > 0) {
            totalHotelsRemaining--;
            return true;
        }
        return false;
    }

    public void returnHotel() {
        if (totalHotelsRemaining < 12) {
            totalHotelsRemaining++;
        }
    }
}
