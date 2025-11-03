package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Builder
@Data
@RequiredArgsConstructor
public class Room {
    private final UUID roomId;
    private final String roomName;
    private final String[] players;
    private final boolean isStarted;
}
