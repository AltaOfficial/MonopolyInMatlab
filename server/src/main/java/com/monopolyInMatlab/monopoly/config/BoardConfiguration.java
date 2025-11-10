package com.monopolyInMatlab.monopoly.config;

import com.monopolyInMatlab.monopoly.domain.*;

import java.util.ArrayList;
import java.util.List;

public class BoardConfiguration {

    public static List<BoardSpace> createStandardBoard() {
        List<BoardSpace> spaces = new ArrayList<>(40);

        // Position 0: GO
        spaces.add(SpecialSpace.builder()
                .position(0)
                .name("GO")
                .spaceType(SpaceType.CORNER)
                .build());

        // Position 1: Mediterranean Avenue (Brown)
        spaces.add(PropertySpace.builder()
                .position(1)
                .name("Mediterranean Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.BROWN)
                .purchasePrice(60)
                .mortgageValue(30)
                .rentBase(2)
                .rent1House(10)
                .rent2House(30)
                .rent3House(90)
                .rent4House(160)
                .rentHotel(250)
                .houseCost(50)
                .hotelCost(50)
                .build());

        // Position 2: Community Chest
        spaces.add(SpecialSpace.builder()
                .position(2)
                .name("Community Chest")
                .spaceType(SpaceType.COMMUNITY_CHEST)
                .build());

        // Position 3: Baltic Avenue (Brown)
        spaces.add(PropertySpace.builder()
                .position(3)
                .name("Baltic Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.BROWN)
                .purchasePrice(60)
                .mortgageValue(30)
                .rentBase(4)
                .rent1House(20)
                .rent2House(60)
                .rent3House(180)
                .rent4House(320)
                .rentHotel(450)
                .houseCost(50)
                .hotelCost(50)
                .build());

        // Position 4: Income Tax
        spaces.add(SpecialSpace.builder()
                .position(4)
                .name("Income Tax")
                .spaceType(SpaceType.TAX)
                .taxAmount(200)
                .build());

        // Position 5: Reading Railroad
        spaces.add(RailroadSpace.builder()
                .position(5)
                .name("Reading Railroad")
                .spaceType(SpaceType.RAILROAD)
                .purchasePrice(200)
                .mortgageValue(100)
                .build());

        // Position 6: Oriental Avenue (Light Blue)
        spaces.add(PropertySpace.builder()
                .position(6)
                .name("Oriental Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.LIGHT_BLUE)
                .purchasePrice(100)
                .mortgageValue(50)
                .rentBase(6)
                .rent1House(30)
                .rent2House(90)
                .rent3House(270)
                .rent4House(400)
                .rentHotel(550)
                .houseCost(50)
                .hotelCost(50)
                .build());

        // Position 7: Chance
        spaces.add(SpecialSpace.builder()
                .position(7)
                .name("Chance")
                .spaceType(SpaceType.CHANCE)
                .build());

        // Position 8: Vermont Avenue (Light Blue)
        spaces.add(PropertySpace.builder()
                .position(8)
                .name("Vermont Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.LIGHT_BLUE)
                .purchasePrice(100)
                .mortgageValue(50)
                .rentBase(6)
                .rent1House(30)
                .rent2House(90)
                .rent3House(270)
                .rent4House(400)
                .rentHotel(550)
                .houseCost(50)
                .hotelCost(50)
                .build());

        // Position 9: Connecticut Avenue (Light Blue)
        spaces.add(PropertySpace.builder()
                .position(9)
                .name("Connecticut Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.LIGHT_BLUE)
                .purchasePrice(120)
                .mortgageValue(60)
                .rentBase(8)
                .rent1House(40)
                .rent2House(100)
                .rent3House(300)
                .rent4House(450)
                .rentHotel(600)
                .houseCost(50)
                .hotelCost(50)
                .build());

        // Position 10: Jail/Just Visiting
        spaces.add(SpecialSpace.builder()
                .position(10)
                .name("Jail / Just Visiting")
                .spaceType(SpaceType.CORNER)
                .build());

        // Position 11: St. Charles Place (Pink)
        spaces.add(PropertySpace.builder()
                .position(11)
                .name("St. Charles Place")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.PINK)
                .purchasePrice(140)
                .mortgageValue(70)
                .rentBase(10)
                .rent1House(50)
                .rent2House(150)
                .rent3House(450)
                .rent4House(625)
                .rentHotel(750)
                .houseCost(100)
                .hotelCost(100)
                .build());

        // Position 12: Electric Company (Utility)
        spaces.add(UtilitySpace.builder()
                .position(12)
                .name("Electric Company")
                .spaceType(SpaceType.UTILITY)
                .purchasePrice(150)
                .mortgageValue(75)
                .build());

        // Position 13: States Avenue (Pink)
        spaces.add(PropertySpace.builder()
                .position(13)
                .name("States Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.PINK)
                .purchasePrice(140)
                .mortgageValue(70)
                .rentBase(10)
                .rent1House(50)
                .rent2House(150)
                .rent3House(450)
                .rent4House(625)
                .rentHotel(750)
                .houseCost(100)
                .hotelCost(100)
                .build());

        // Position 14: Virginia Avenue (Pink)
        spaces.add(PropertySpace.builder()
                .position(14)
                .name("Virginia Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.PINK)
                .purchasePrice(160)
                .mortgageValue(80)
                .rentBase(12)
                .rent1House(60)
                .rent2House(180)
                .rent3House(500)
                .rent4House(700)
                .rentHotel(900)
                .houseCost(100)
                .hotelCost(100)
                .build());

        // Position 15: Pennsylvania Railroad
        spaces.add(RailroadSpace.builder()
                .position(15)
                .name("Pennsylvania Railroad")
                .spaceType(SpaceType.RAILROAD)
                .purchasePrice(200)
                .mortgageValue(100)
                .build());

        // Position 16: St. James Place (Orange)
        spaces.add(PropertySpace.builder()
                .position(16)
                .name("St. James Place")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.ORANGE)
                .purchasePrice(180)
                .mortgageValue(90)
                .rentBase(14)
                .rent1House(70)
                .rent2House(200)
                .rent3House(550)
                .rent4House(750)
                .rentHotel(950)
                .houseCost(100)
                .hotelCost(100)
                .build());

        // Position 17: Community Chest
        spaces.add(SpecialSpace.builder()
                .position(17)
                .name("Community Chest")
                .spaceType(SpaceType.COMMUNITY_CHEST)
                .build());

        // Position 18: Tennessee Avenue (Orange)
        spaces.add(PropertySpace.builder()
                .position(18)
                .name("Tennessee Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.ORANGE)
                .purchasePrice(180)
                .mortgageValue(90)
                .rentBase(14)
                .rent1House(70)
                .rent2House(200)
                .rent3House(550)
                .rent4House(750)
                .rentHotel(950)
                .houseCost(100)
                .hotelCost(100)
                .build());

        // Position 19: New York Avenue (Orange)
        spaces.add(PropertySpace.builder()
                .position(19)
                .name("New York Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.ORANGE)
                .purchasePrice(200)
                .mortgageValue(100)
                .rentBase(16)
                .rent1House(80)
                .rent2House(220)
                .rent3House(600)
                .rent4House(800)
                .rentHotel(1000)
                .houseCost(100)
                .hotelCost(100)
                .build());

        // Position 20: Free Parking
        spaces.add(SpecialSpace.builder()
                .position(20)
                .name("Free Parking")
                .spaceType(SpaceType.CORNER)
                .build());

        // Position 21: Kentucky Avenue (Red)
        spaces.add(PropertySpace.builder()
                .position(21)
                .name("Kentucky Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.RED)
                .purchasePrice(220)
                .mortgageValue(110)
                .rentBase(18)
                .rent1House(90)
                .rent2House(250)
                .rent3House(700)
                .rent4House(875)
                .rentHotel(1050)
                .houseCost(150)
                .hotelCost(150)
                .build());

        // Position 22: Chance
        spaces.add(SpecialSpace.builder()
                .position(22)
                .name("Chance")
                .spaceType(SpaceType.CHANCE)
                .build());

        // Position 23: Indiana Avenue (Red)
        spaces.add(PropertySpace.builder()
                .position(23)
                .name("Indiana Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.RED)
                .purchasePrice(220)
                .mortgageValue(110)
                .rentBase(18)
                .rent1House(90)
                .rent2House(250)
                .rent3House(700)
                .rent4House(875)
                .rentHotel(1050)
                .houseCost(150)
                .hotelCost(150)
                .build());

        // Position 24: Illinois Avenue (Red)
        spaces.add(PropertySpace.builder()
                .position(24)
                .name("Illinois Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.RED)
                .purchasePrice(240)
                .mortgageValue(120)
                .rentBase(20)
                .rent1House(100)
                .rent2House(300)
                .rent3House(750)
                .rent4House(925)
                .rentHotel(1100)
                .houseCost(150)
                .hotelCost(150)
                .build());

        // Position 25: B&O Railroad
        spaces.add(RailroadSpace.builder()
                .position(25)
                .name("B&O Railroad")
                .spaceType(SpaceType.RAILROAD)
                .purchasePrice(200)
                .mortgageValue(100)
                .build());

        // Position 26: Atlantic Avenue (Yellow)
        spaces.add(PropertySpace.builder()
                .position(26)
                .name("Atlantic Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.YELLOW)
                .purchasePrice(260)
                .mortgageValue(130)
                .rentBase(22)
                .rent1House(110)
                .rent2House(330)
                .rent3House(800)
                .rent4House(975)
                .rentHotel(1150)
                .houseCost(150)
                .hotelCost(150)
                .build());

        // Position 27: Ventnor Avenue (Yellow)
        spaces.add(PropertySpace.builder()
                .position(27)
                .name("Ventnor Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.YELLOW)
                .purchasePrice(260)
                .mortgageValue(130)
                .rentBase(22)
                .rent1House(110)
                .rent2House(330)
                .rent3House(800)
                .rent4House(975)
                .rentHotel(1150)
                .houseCost(150)
                .hotelCost(150)
                .build());

        // Position 28: Water Works (Utility)
        spaces.add(UtilitySpace.builder()
                .position(28)
                .name("Water Works")
                .spaceType(SpaceType.UTILITY)
                .purchasePrice(150)
                .mortgageValue(75)
                .build());

        // Position 29: Marvin Gardens (Yellow)
        spaces.add(PropertySpace.builder()
                .position(29)
                .name("Marvin Gardens")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.YELLOW)
                .purchasePrice(280)
                .mortgageValue(140)
                .rentBase(24)
                .rent1House(120)
                .rent2House(360)
                .rent3House(850)
                .rent4House(1025)
                .rentHotel(1200)
                .houseCost(150)
                .hotelCost(150)
                .build());

        // Position 30: Go To Jail
        spaces.add(SpecialSpace.builder()
                .position(30)
                .name("Go To Jail")
                .spaceType(SpaceType.CORNER)
                .build());

        // Position 31: Pacific Avenue (Green)
        spaces.add(PropertySpace.builder()
                .position(31)
                .name("Pacific Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.GREEN)
                .purchasePrice(300)
                .mortgageValue(150)
                .rentBase(26)
                .rent1House(130)
                .rent2House(390)
                .rent3House(900)
                .rent4House(1100)
                .rentHotel(1275)
                .houseCost(200)
                .hotelCost(200)
                .build());

        // Position 32: North Carolina Avenue (Green)
        spaces.add(PropertySpace.builder()
                .position(32)
                .name("North Carolina Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.GREEN)
                .purchasePrice(300)
                .mortgageValue(150)
                .rentBase(26)
                .rent1House(130)
                .rent2House(390)
                .rent3House(900)
                .rent4House(1100)
                .rentHotel(1275)
                .houseCost(200)
                .hotelCost(200)
                .build());

        // Position 33: Community Chest
        spaces.add(SpecialSpace.builder()
                .position(33)
                .name("Community Chest")
                .spaceType(SpaceType.COMMUNITY_CHEST)
                .build());

        // Position 34: Pennsylvania Avenue (Green)
        spaces.add(PropertySpace.builder()
                .position(34)
                .name("Pennsylvania Avenue")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.GREEN)
                .purchasePrice(320)
                .mortgageValue(160)
                .rentBase(28)
                .rent1House(150)
                .rent2House(450)
                .rent3House(1000)
                .rent4House(1200)
                .rentHotel(1400)
                .houseCost(200)
                .hotelCost(200)
                .build());

        // Position 35: Short Line Railroad
        spaces.add(RailroadSpace.builder()
                .position(35)
                .name("Short Line")
                .spaceType(SpaceType.RAILROAD)
                .purchasePrice(200)
                .mortgageValue(100)
                .build());

        // Position 36: Chance
        spaces.add(SpecialSpace.builder()
                .position(36)
                .name("Chance")
                .spaceType(SpaceType.CHANCE)
                .build());

        // Position 37: Park Place (Dark Blue)
        spaces.add(PropertySpace.builder()
                .position(37)
                .name("Park Place")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.DARK_BLUE)
                .purchasePrice(350)
                .mortgageValue(175)
                .rentBase(35)
                .rent1House(175)
                .rent2House(500)
                .rent3House(1100)
                .rent4House(1300)
                .rentHotel(1500)
                .houseCost(200)
                .hotelCost(200)
                .build());

        // Position 38: Luxury Tax
        spaces.add(SpecialSpace.builder()
                .position(38)
                .name("Luxury Tax")
                .spaceType(SpaceType.TAX)
                .taxAmount(100)
                .build());

        // Position 39: Boardwalk (Dark Blue)
        spaces.add(PropertySpace.builder()
                .position(39)
                .name("Boardwalk")
                .spaceType(SpaceType.PROPERTY)
                .colorGroup(ColorGroup.DARK_BLUE)
                .purchasePrice(400)
                .mortgageValue(200)
                .rentBase(50)
                .rent1House(200)
                .rent2House(600)
                .rent3House(1400)
                .rent4House(1700)
                .rentHotel(2000)
                .houseCost(200)
                .hotelCost(200)
                .build());

        return spaces;
    }

    private BoardConfiguration() {
        // Utility class
    }
}
