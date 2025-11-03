package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@Builder
@RequiredArgsConstructor
public class Player {
    private final UUID playerId;
    private final String playerName;
}
