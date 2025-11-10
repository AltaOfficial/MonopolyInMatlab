package com.monopolyInMatlab.monopoly.service;

import com.monopolyInMatlab.monopoly.domain.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    ChatMessage sendMessage(UUID roomId, UUID playerId, String playerName, String message);
    List<ChatMessage> getMessageHistory(UUID roomId);
}
