package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Card {
    private final String cardType;  // "CHANCE" or "COMMUNITY_CHEST"
    private final String description;
    private final CardActionType actionType;
    private final Integer value;  // Amount for money cards, position for movement cards, etc.
}
