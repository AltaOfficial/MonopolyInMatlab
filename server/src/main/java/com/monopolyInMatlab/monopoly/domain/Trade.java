package com.monopolyInMatlab.monopoly.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class Trade {
    private final UUID tradeId;
    private final UUID fromPlayerId;
    private final UUID toPlayerId;
    private final List<Integer> fromPlayerProperties;  // positions
    private final int fromPlayerMoney;
    private final List<Integer> toPlayerProperties;  // positions
    private final int toPlayerMoney;

    @Builder.Default
    private TradeStatus status = TradeStatus.PENDING;

    public enum TradeStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}
