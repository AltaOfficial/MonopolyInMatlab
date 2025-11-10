package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
public class Player {
    private final UUID playerId;
    private final String playerName;
}
