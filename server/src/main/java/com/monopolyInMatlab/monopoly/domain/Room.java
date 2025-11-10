package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
public class Room {
    private final UUID roomId;
    private final String roomName;
    private final List<Player> players;
    private final boolean isStarted;
}
