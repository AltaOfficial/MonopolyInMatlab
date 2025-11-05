package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@RequiredArgsConstructor
public class Room {
    @Builder.Default
    private final UUID roomId = UUID.randomUUID();
    private final String roomName;

    @Builder.Default
    private final List<Player> players = new LinkedList<>();

    @Builder.Default
    private final boolean isStarted = false;
}
