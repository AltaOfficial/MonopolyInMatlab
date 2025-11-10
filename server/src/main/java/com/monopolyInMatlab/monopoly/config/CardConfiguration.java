package com.monopolyInMatlab.monopoly.config;

import com.monopolyInMatlab.monopoly.domain.Card;
import com.monopolyInMatlab.monopoly.domain.CardActionType;

import java.util.ArrayList;
import java.util.List;

public class CardConfiguration {

    public static List<Card> createChanceCards() {
        List<Card> cards = new ArrayList<>();

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Advance to Go (Collect $200)")
                .actionType(CardActionType.ADVANCE_TO_GO)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Advance to Illinois Avenue")
                .actionType(CardActionType.ADVANCE_TO_SPACE)
                .value(24)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Advance to St. Charles Place")
                .actionType(CardActionType.ADVANCE_TO_SPACE)
                .value(11)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Advance token to nearest Utility")
                .actionType(CardActionType.ADVANCE_TO_SPACE)
                .value(-1)  // Special: nearest utility
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Advance token to nearest Railroad")
                .actionType(CardActionType.ADVANCE_TO_SPACE)
                .value(-2)  // Special: nearest railroad
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Bank pays you dividend of $50")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(50)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Get Out of Jail Free")
                .actionType(CardActionType.GET_OUT_OF_JAIL_FREE)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Go Back 3 Spaces")
                .actionType(CardActionType.GO_BACK_SPACES)
                .value(3)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Go to Jail")
                .actionType(CardActionType.GO_TO_JAIL)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Make general repairs on all your property ($25 per house, $100 per hotel)")
                .actionType(CardActionType.PAY_PER_HOUSE_HOTEL)
                .value(25)  // Store house cost, hotel cost = 4x
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Pay poor tax of $15")
                .actionType(CardActionType.PAY_MONEY)
                .value(15)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Take a trip to Reading Railroad")
                .actionType(CardActionType.ADVANCE_TO_SPACE)
                .value(5)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Take a walk on the Boardwalk")
                .actionType(CardActionType.ADVANCE_TO_SPACE)
                .value(39)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("You have been elected Chairman of the Board (Pay each player $50)")
                .actionType(CardActionType.PAY_TO_PLAYERS)
                .value(50)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("Your building loan matures (Collect $150)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(150)
                .build());

        cards.add(Card.builder()
                .cardType("CHANCE")
                .description("You have won a crossword competition (Collect $100)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(100)
                .build());

        return cards;
    }

    public static List<Card> createCommunityChestCards() {
        List<Card> cards = new ArrayList<>();

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Advance to Go (Collect $200)")
                .actionType(CardActionType.ADVANCE_TO_GO)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Bank error in your favor (Collect $200)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(200)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Doctor's fees (Pay $50)")
                .actionType(CardActionType.PAY_MONEY)
                .value(50)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("From sale of stock you get $50")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(50)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Get Out of Jail Free")
                .actionType(CardActionType.GET_OUT_OF_JAIL_FREE)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Go to Jail")
                .actionType(CardActionType.GO_TO_JAIL)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Grand Opera Night (Collect $50 from every player)")
                .actionType(CardActionType.COLLECT_FROM_PLAYERS)
                .value(50)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Holiday Fund matures (Collect $100)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(100)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Income tax refund (Collect $20)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(20)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("It is your birthday (Collect $10 from every player)")
                .actionType(CardActionType.COLLECT_FROM_PLAYERS)
                .value(10)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Life insurance matures (Collect $100)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(100)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Hospital fees (Pay $100)")
                .actionType(CardActionType.PAY_MONEY)
                .value(100)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("School fees (Pay $150)")
                .actionType(CardActionType.PAY_MONEY)
                .value(150)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("Receive $25 consultancy fee")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(25)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("You are assessed for street repairs ($40 per house, $115 per hotel)")
                .actionType(CardActionType.PAY_PER_HOUSE_HOTEL)
                .value(40)  // Store house cost, hotel cost = 2.875x (use 115)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("You have won second prize in a beauty contest (Collect $10)")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(10)
                .build());

        cards.add(Card.builder()
                .cardType("COMMUNITY_CHEST")
                .description("You inherit $100")
                .actionType(CardActionType.COLLECT_MONEY)
                .value(100)
                .build());

        return cards;
    }

    private CardConfiguration() {
        // Utility class
    }
}
