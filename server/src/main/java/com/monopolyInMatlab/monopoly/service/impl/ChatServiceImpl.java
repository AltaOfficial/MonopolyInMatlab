package com.monopolyInMatlab.monopoly.service.impl;

import com.monopolyInMatlab.monopoly.domain.ChatMessage;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import com.monopolyInMatlab.monopoly.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final RoomRepository roomRepository;

    @Override
    public ChatMessage sendMessage(UUID roomId, UUID playerId, String playerName, String message) {
        ChatMessage chatMessage = ChatMessage.builder()
                .playerId(playerId)
                .playerName(playerName)
                .message(message)
                .build();

        // Get room and add chat message
        GameRoom room = (GameRoom) roomRepository.getAllRooms().stream()
                .filter(r -> r.getRoomId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        room.addChatMessage(chatMessage);

        return chatMessage;
    }

    @Override
    public List<ChatMessage> getMessageHistory(UUID roomId) {
        GameRoom room = (GameRoom) roomRepository.getAllRooms().stream()
                .filter(r -> r.getRoomId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        return room.getChatHistory();
    }
}
