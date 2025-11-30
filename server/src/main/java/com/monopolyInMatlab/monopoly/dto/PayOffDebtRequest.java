package com.monopolyInMatlab.monopoly.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PayOffDebtRequest {
    private UUID playerId;
    private List<Integer> housesToSell;           // Positions of properties to sell houses from
    private List<Integer> hotelsToSell;           // Positions of properties to sell hotels from
    private List<Integer> propertiesToMortgage;   // Positions of properties to mortgage
    private UUID creditorId;                      // Player to pay, or null for bank
    private Integer amountOwed;                   // Total amount that needs to be paid
}
