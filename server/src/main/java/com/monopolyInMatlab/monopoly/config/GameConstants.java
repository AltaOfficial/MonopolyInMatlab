package com.monopolyInMatlab.monopoly.config;

public class GameConstants {
    // Starting values
    public static final int STARTING_MONEY = 1500;
    public static final int STARTING_POSITION = 0;  // GO

    // Special positions
    public static final int GO_POSITION = 0;
    public static final int JAIL_POSITION = 10;
    public static final int GO_TO_JAIL_POSITION = 30;
    public static final int FREE_PARKING_POSITION = 20;

    // Money amounts
    public static final int GO_SALARY = 200;
    public static final int JAIL_FINE = 50;
    public static final int INCOME_TAX = 200;
    public static final int LUXURY_TAX = 100;

    // Jail rules
    public static final int MAX_JAIL_TURNS = 3;
    public static final int MAX_DOUBLES_BEFORE_JAIL = 3;

    // Building limits
    public static final int MAX_HOUSES = 32;
    public static final int MAX_HOTELS = 12;
    public static final int HOUSES_BEFORE_HOTEL = 4;

    // Mortgage
    public static final double UNMORTGAGE_RATE = 1.1;  // Pay 110% to unmortgage

    private GameConstants() {
        // Utility class, no instantiation
    }
}
