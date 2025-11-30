package com.monopolyInMatlab.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiquidationAsset {
    private String type;          // "HOUSE", "HOTEL", "PROPERTY"
    private Integer position;     // Board position
    private String propertyName;  // Name of the property
    private Integer value;        // Cash value if sold/mortgaged
    private Integer count;        // Number of houses (if type is HOUSE)
}
