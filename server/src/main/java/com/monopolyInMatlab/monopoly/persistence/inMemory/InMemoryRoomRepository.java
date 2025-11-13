package com.monopolyInMatlab.monopoly.persistence.inMemory;

import com.monopolyInMatlab.monopoly.config.BoardConfiguration;
import com.monopolyInMatlab.monopoly.config.CardConfiguration;
import com.monopolyInMatlab.monopoly.domain.Card;
import com.monopolyInMatlab.monopoly.domain.CreateRoomRequest;
import com.monopolyInMatlab.monopoly.domain.GameRoom;
import com.monopolyInMatlab.monopoly.domain.GamePlayer;
import com.monopolyInMatlab.monopoly.persistence.RoomRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryRoomRepository implements RoomRepository {
    private final ConcurrentMap<UUID, GameRoom> gameRooms = new ConcurrentHashMap<>();

    @Override
    public List<GameRoom> getAllRooms() {
        return new ArrayList<>(gameRooms.values());
    }

    @Override
    public GameRoom createRoom(CreateRoomRequest createRoomRequest) {
        // Initialize card decks
        List<Card> chanceCards = CardConfiguration.createChanceCards();
        List<Card> communityChestCards = CardConfiguration.createCommunityChestCards();
        Collections.shuffle(chanceCards);
        Collections.shuffle(communityChestCards);

        GameRoom newRoom = GameRoom.builder()
                .roomId(UUID.randomUUID())
                .roomName(createRoomRequest.getRoomName())
                .gamePlayers(new ArrayList<>())
                .isStarted(false)
                .boardSpaces(BoardConfiguration.createStandardBoard())
                .chanceCards(chanceCards)
                .communityChestCards(communityChestCards)
                .build();
        gameRooms.put(newRoom.getRoomId(), newRoom);
        return newRoom;
    }

    @Override
    public void joinRoom(UUID roomId, GamePlayer player) {
        GameRoom room = gameRooms.get(roomId);
        if (room != null && !room.isStarted()) {
            room.getGamePlayers().add(GamePlayer.builder().playerName(player.getPlayerName()).build());
        }
    }

    @Override
    public GameRoom findGameRoomById(UUID roomId) {
        return gameRooms.get(roomId);
    }

    @Override
    public void saveGameRoom(GameRoom gameRoom) {
        gameRooms.put(gameRoom.getRoomId(), gameRoom);
    }

    @Override
    public void deleteRoom(UUID roomId) {
        gameRooms.remove(roomId);
    }
}
