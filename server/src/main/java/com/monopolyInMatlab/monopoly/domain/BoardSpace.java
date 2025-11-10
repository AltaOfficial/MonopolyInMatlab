package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class BoardSpace {
    private final int position;  // 0-39
    private final String name;
    private final SpaceType spaceType;
}
