package com.monopolyInMatlab.monopoly.presentation;

import com.monopolyInMatlab.monopoly.domain.*;
import com.monopolyInMatlab.monopoly.dto.*;
import com.monopolyInMatlab.monopoly.service.ChatService;
import com.monopolyInMatlab.monopoly.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GameWebsocketController {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final GameService gameService;
    private final ChatService chatService;

    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, @Payload GamePlayer player) {
        System.out.println("Player " + player.getPlayerName() + " joined room " + roomId);

        // Note: RoomsService.joinRoom should be called from client before websocket connection
        // This message handler just broadcasts the join event
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", player.getPlayerName());

        broadcastGameEvent(roomId, "PLAYER_JOINED", data);
    }

    // Chat endpoint
    @MessageMapping("/room/{roomId}/chat")
    public void handleChatMessage(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        UUID playerId = UUID.fromString(payload.get("playerId"));
        String playerName = payload.get("playerName");
        String message = payload.get("message");

        System.out.println("Player " + playerName + " sent a message:" + message);

        ChatMessage chatMessage = chatService.sendMessage(UUID.fromString(roomId), playerId, playerName, message);

        ChatMessageDTO dto = ChatMessageDTO.builder()
                .messageType("CHAT_MESSAGE")
                .playerId(chatMessage.getPlayerId())
                .playerName(chatMessage.getPlayerName())
                .message(chatMessage.getMessage())
                .build();

        simpMessagingTemplate.convertAndSend("/room/" + roomId, dto);
    }

    // Game lifecycle
    @MessageMapping("/room/{roomId}/game/start")
    public void startGame(@DestinationVariable String roomId) {
        try {
            gameService.initializeGame(UUID.fromString(roomId));
            GameRoom room = gameService.startGame(UUID.fromString(roomId));

            Map<String, Object> data = new HashMap<>();
            data.put("currentPlayerIndex", room.getCurrentPlayerIndex());
            data.put("currentPlayerId", room.getCurrentPlayer().getPlayerId().toString());

            broadcastGameEvent(roomId, "GAME_STARTED", data);
        } catch (Exception e) {
            broadcastError(roomId, "Failed to start game: " + e.getMessage());
        }
    }

    // Turn actions
    @MessageMapping("/room/{roomId}/game/roll")
    public void rollDice(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            int[] dice = gameService.rollDice(UUID.fromString(roomId), request.getPlayerId());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("dice", new int[]{dice[0], dice[1]});
            data.put("isDoubles", dice[0] == dice[1]);

            broadcastGameEvent(roomId, "DICE_ROLLED", data);

            // Get updated player position
            GameRoom room = gameService.getGameRoom(UUID.fromString(roomId));
            GamePlayer player = room.getPlayerById(request.getPlayerId());

            Map<String, Object> moveData = new HashMap<>();
            moveData.put("playerId", request.getPlayerId().toString());
            moveData.put("newPosition", player.getPosition());
            moveData.put("money", player.getMoney());

            broadcastGameEvent(roomId, "PLAYER_MOVED", moveData);

            // Check if player needs to liquidate assets to pay debt
            checkAndBroadcastLiquidationRequired(room, roomId);

        } catch (Exception e) {
            broadcastError(roomId, "Roll dice failed: " + e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/game/endTurn")
    public void endTurn(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.endTurn(UUID.fromString(roomId), request.getPlayerId());
            GameRoom room = gameService.getGameRoom(UUID.fromString(roomId));

            if (room.getGamePhase() == GamePhase.FINISHED) {
                Map<String, Object> data = new HashMap<>();
                data.put("winnerId", room.getWinnerId().toString());
                broadcastGameEvent(roomId, "GAME_OVER", data);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("currentPlayerId", room.getCurrentPlayer().getPlayerId().toString());
                broadcastGameEvent(roomId, "TURN_CHANGED", data);
            }
        } catch (Exception e) {
            broadcastError(roomId, "End turn failed: " + e.getMessage());
        }
    }

    // Property actions
    @MessageMapping("/room/{roomId}/game/buyProperty")
    public void buyProperty(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.buyProperty(UUID.fromString(roomId), request.getPlayerId(), request.getPosition());
            GameRoom room = gameService.getGameRoom(UUID.fromString(roomId));
            GamePlayer player = room.getPlayerById(request.getPlayerId());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("position", request.getPosition());
            data.put("money", player.getMoney());

            broadcastGameEvent(roomId, "PROPERTY_BOUGHT", data);
        } catch (Exception e) {
            broadcastError(roomId, "Buy property failed: " + e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/game/declineProperty")
    public void declineProperty(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.declineProperty(UUID.fromString(roomId), request.getPlayerId(), request.getPosition());

            Map<String, Object> data = new HashMap<>();
            data.put("position", request.getPosition());

            broadcastGameEvent(roomId, "AUCTION_STARTED", data);
        } catch (Exception e) {
            broadcastError(roomId, "Decline property failed: " + e.getMessage());
        }
    }

    // Building actions
    @MessageMapping("/room/{roomId}/game/buildHouse")
    public void buildHouse(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.buildHouse(UUID.fromString(roomId), request.getPlayerId(), request.getPosition());
            GameRoom room = gameService.getGameRoom(UUID.fromString(roomId));
            PropertySpace prop = (PropertySpace) room.getBoardSpaces().get(request.getPosition());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("position", request.getPosition());
            data.put("housesBuilt", prop.getHousesBuilt());

            broadcastGameEvent(roomId, "HOUSE_BUILT", data);
        } catch (Exception e) {
            broadcastError(roomId, "Build house failed: " + e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/game/buildHotel")
    public void buildHotel(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.buildHotel(UUID.fromString(roomId), request.getPlayerId(), request.getPosition());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("position", request.getPosition());

            broadcastGameEvent(roomId, "HOTEL_BUILT", data);
        } catch (Exception e) {
            broadcastError(roomId, "Build hotel failed: " + e.getMessage());
        }
    }

    // Mortgage actions
    @MessageMapping("/room/{roomId}/game/mortgage")
    public void mortgageProperty(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.mortgageProperty(UUID.fromString(roomId), request.getPlayerId(), request.getPosition());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("position", request.getPosition());

            broadcastGameEvent(roomId, "PROPERTY_MORTGAGED", data);
        } catch (Exception e) {
            broadcastError(roomId, "Mortgage failed: " + e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/game/unmortgage")
    public void unmortgageProperty(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.unmortgageProperty(UUID.fromString(roomId), request.getPlayerId(), request.getPosition());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("position", request.getPosition());

            broadcastGameEvent(roomId, "PROPERTY_UNMORTGAGED", data);
        } catch (Exception e) {
            broadcastError(roomId, "Unmortgage failed: " + e.getMessage());
        }
    }

    // Jail actions
    @MessageMapping("/room/{roomId}/game/jailAction")
    public void handleJailAction(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            String action = request.getAction();
            boolean released = false;

            switch (action) {
                case "PAY":
                    gameService.payJailFine(UUID.fromString(roomId), request.getPlayerId());
                    released = true;
                    break;
                case "CARD":
                    gameService.useGetOutOfJailCard(UUID.fromString(roomId), request.getPlayerId());
                    released = true;
                    break;
                case "ROLL":
                    released = gameService.rollForJail(UUID.fromString(roomId), request.getPlayerId());
                    break;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("released", released);

            broadcastGameEvent(roomId, "PLAYER_RELEASED_JAIL", data);
        } catch (Exception e) {
            broadcastError(roomId, "Jail action failed: " + e.getMessage());
        }
    }

    // Trading
    @MessageMapping("/room/{roomId}/game/proposeTrade")
    public void proposeTrade(@DestinationVariable String roomId, @Payload TradeOfferDTO tradeOffer) {
        try {
            gameService.proposeTrade(UUID.fromString(roomId), tradeOffer);

            Map<String, Object> data = new HashMap<>();
            data.put("tradeId", tradeOffer.getTradeId().toString());
            data.put("fromPlayerId", tradeOffer.getFromPlayerId().toString());
            data.put("toPlayerId", tradeOffer.getToPlayerId().toString());

            broadcastGameEvent(roomId, "TRADE_PROPOSED", data);
        } catch (Exception e) {
            broadcastError(roomId, "Propose trade failed: " + e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/game/respondTrade")
    public void respondToTrade(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        try {
            UUID playerId = UUID.fromString(payload.get("playerId"));
            UUID tradeId = UUID.fromString(payload.get("tradeId"));
            boolean accept = Boolean.parseBoolean(payload.get("accept"));

            gameService.respondToTrade(UUID.fromString(roomId), playerId, tradeId, accept);

            Map<String, Object> data = new HashMap<>();
            data.put("tradeId", tradeId.toString());
            data.put("accepted", accept);

            String eventType = accept ? "TRADE_COMPLETED" : "TRADE_DECLINED";
            broadcastGameEvent(roomId, eventType, data);
        } catch (Exception e) {
            broadcastError(roomId, "Respond to trade failed: " + e.getMessage());
        }
    }

    // Auction
    @MessageMapping("/room/{roomId}/game/placeBid")
    public void placeBid(@DestinationVariable String roomId, @Payload GameActionRequest request) {
        try {
            gameService.placeBid(UUID.fromString(roomId), request.getPlayerId(), request.getAmount());

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("amount", request.getAmount());

            broadcastGameEvent(roomId, "BID_PLACED", data);
        } catch (Exception e) {
            broadcastError(roomId, "Place bid failed: " + e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/game/endAuction")
    public void endAuction(@DestinationVariable String roomId) {
        try {
            gameService.endAuction(UUID.fromString(roomId));

            broadcastGameEvent(roomId, "AUCTION_ENDED", new HashMap<>());
        } catch (Exception e) {
            broadcastError(roomId, "End auction failed: " + e.getMessage());
        }
    }

    // Card handling
    @MessageMapping("/room/{roomId}/game/drawCard")
    public void drawCard(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        try {
            UUID playerId = UUID.fromString(payload.get("playerId"));
            String deckType = payload.get("deckType");

            Card card = gameService.drawCard(UUID.fromString(roomId), playerId, deckType);

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", playerId.toString());
            data.put("cardType", card.getCardType());
            data.put("description", card.getDescription());

            broadcastGameEvent(roomId, "CARD_DRAWN", data);

            // Check if card action requires liquidation
            GameRoom room = gameService.getGameRoom(UUID.fromString(roomId));
            checkAndBroadcastLiquidationRequired(room, roomId);
        } catch (Exception e) {
            broadcastError(roomId, "Draw card failed: " + e.getMessage());
        }
    }

    // Debt payment handling
    @MessageMapping("/room/{roomId}/game/payOffDebt")
    public void payOffDebt(@DestinationVariable String roomId, @Payload PayOffDebtRequest request) {
        try {
            gameService.payOffDebt(
                UUID.fromString(roomId),
                request.getPlayerId(),
                request.getHousesToSell(),
                request.getHotelsToSell(),
                request.getPropertiesToMortgage(),
                request.getCreditorId(),
                request.getAmountOwed()
            );

            // Get updated game state
            GameRoom room = gameService.getGameRoom(UUID.fromString(roomId));
            GamePlayer player = room.getPlayerById(request.getPlayerId());

            // Clear pending debt
            room.clearPendingDebt();

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", request.getPlayerId().toString());
            data.put("playerMoney", player.getMoney());
            data.put("amountPaid", request.getAmountOwed());
            if (request.getCreditorId() != null) {
                GamePlayer creditor = room.getPlayerById(request.getCreditorId());
                data.put("creditorId", request.getCreditorId().toString());
                data.put("creditorMoney", creditor.getMoney());
            }

            broadcastGameEvent(roomId, "DEBT_PAID", data);
        } catch (Exception e) {
            broadcastError(roomId, "Pay off debt failed: " + e.getMessage());
        }
    }

    // Helper methods
    private void broadcastGameEvent(String roomId, String eventType, Map<String, Object> data) {
        GameEventMessage message = GameEventMessage.builder()
                .messageType(eventType)
                .data(data)
                .build();

        simpMessagingTemplate.convertAndSend("/room/" + roomId, message);
    }

    private void broadcastError(String roomId, String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("error", errorMessage);

        broadcastGameEvent(roomId, "ERROR", data);
    }

    private void checkAndBroadcastLiquidationRequired(GameRoom room, String roomId) {
        if (room.getPendingDebtAmount() != null && room.getPendingDebtPlayerId() != null) {
            GamePlayer debtor = room.getPlayerById(room.getPendingDebtPlayerId());

            // Build list of liquidation assets
            java.util.List<LiquidationAsset> assets = new java.util.ArrayList<>();

            for (int position : debtor.getOwnedPropertyPositions()) {
                BoardSpace space = room.getBoardSpaces().get(position);

                if (space instanceof PropertySpace) {
                    PropertySpace prop = (PropertySpace) space;

                    // Add houses if any
                    if (prop.getHousesBuilt() > 0) {
                        assets.add(new LiquidationAsset("HOUSE", position, prop.getName(),
                                                        prop.getHouseCost() / 2, prop.getHousesBuilt()));
                    }

                    // Add hotel if any
                    if (prop.isHasHotel()) {
                        assets.add(new LiquidationAsset("HOTEL", position, prop.getName(),
                                                        prop.getHotelCost() / 2, 1));
                    }

                    // Add property for mortgaging (if not already mortgaged and no buildings)
                    if (prop.canMortgage()) {
                        assets.add(new LiquidationAsset("PROPERTY", position, prop.getName(),
                                                        prop.getMortgageValue(), 1));
                    }
                } else if (space instanceof RailroadSpace) {
                    RailroadSpace rr = (RailroadSpace) space;
                    if (rr.canMortgage()) {
                        assets.add(new LiquidationAsset("RAILROAD", position, rr.getName(),
                                                        rr.getMortgageValue(), 1));
                    }
                } else if (space instanceof UtilitySpace) {
                    UtilitySpace util = (UtilitySpace) space;
                    if (util.canMortgage()) {
                        assets.add(new LiquidationAsset("UTILITY", position, util.getName(),
                                                        util.getMortgageValue(), 1));
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", room.getPendingDebtPlayerId().toString());
            data.put("amountOwed", room.getPendingDebtAmount());
            data.put("currentMoney", debtor.getMoney());
            data.put("reason", room.getPendingDebtReason());
            if (room.getPendingDebtCreditorId() != null) {
                GamePlayer creditor = room.getPlayerById(room.getPendingDebtCreditorId());
                data.put("creditorId", room.getPendingDebtCreditorId().toString());
                data.put("creditorName", creditor.getPlayerName());
            }
            data.put("assets", assets);

            broadcastGameEvent(roomId, "LIQUIDATION_REQUIRED", data);
        }
    }
}
