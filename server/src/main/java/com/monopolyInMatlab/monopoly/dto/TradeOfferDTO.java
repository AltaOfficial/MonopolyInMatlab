package com.monopolyInMatlab.monopoly.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TradeOfferDTO {
    private UUID tradeId;
    private UUID fromPlayerId;
    private String fromPlayerName;
    private UUID toPlayerId;
    private String toPlayerName;
    private List<Integer> fromPlayerProperties;
    private int fromPlayerMoney;
    private List<Integer> toPlayerProperties;
    private int toPlayerMoney;
}
