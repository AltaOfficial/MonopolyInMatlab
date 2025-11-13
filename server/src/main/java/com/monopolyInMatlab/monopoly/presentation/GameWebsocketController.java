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
        data.put("playerId", player.getPlayerId().toString());
        data.put("playerName", player.getPlayerName());

        broadcastGameEvent(roomId, "PLAYER_JOINED", data);
    }

    // Chat endpoint
    @MessageMapping("/room/{roomId}/chat")
    public void handleChatMessage(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        UUID playerId = UUID.fromString(payload.get("playerId"));
        String playerName = payload.get("playerName");
        String message = payload.get("message");

        ChatMessage chatMessage = chatService.sendMessage(UUID.fromString(roomId), playerId, playerName, message);

        ChatMessageDTO dto = ChatMessageDTO.builder()
                .messageType("CHAT_MESSAGE")
                .playerId(chatMessage.getPlayerId())
                .playerName(chatMessage.getPlayerName())
                .message(chatMessage.getMessage())
                .timestamp(chatMessage.getTimestamp())
                .build();

        simpMessagingTemplate.convertAndSend("/monopoly/room/" + roomId, dto);
    }

    // Game lifecycle
    @MessageMapping("/room/{roomId}/game/start")
    public void startGame(@DestinationVariable String roomId) {
        try {
            GameRoom room = gameService.initializeGame(UUID.fromString(roomId));
            room = gameService.startGame(UUID.fromString(roomId));

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
        } catch (Exception e) {
            broadcastError(roomId, "Draw card failed: " + e.getMessage());
        }
    }

    // Helper methods
    private void broadcastGameEvent(String roomId, String eventType, Map<String, Object> data) {
        GameEventMessage message = GameEventMessage.builder()
                .messageType(eventType)
                .data(data)
                .build();

        simpMessagingTemplate.convertAndSend("/monopoly/room/" + roomId, message);
    }

    private void broadcastError(String roomId, String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("error", errorMessage);

        broadcastGameEvent(roomId, "ERROR", data);
    }
}
