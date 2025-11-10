package com.monopolyInMatlab.monopoly.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SpecialSpace extends BoardSpace {
    private final Integer taxAmount;  // null for non-tax spaces
}
