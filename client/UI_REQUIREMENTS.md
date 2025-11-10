# Monopoly Game - UI Requirements Documentation

This document describes all UI elements needed in the game.mlapp file and how to wire them to the game logic.

## Overview

The game logic is **completely implemented** in the MATLAB classes. You only need to:
1. **Add UI components** to game.mlapp using App Designer
2. **Wire button callbacks** to call the appropriate Game methods
3. **Update displays** when game state changes

---

## UI Layout Sections

### 1. Game Board Display (Center)
**Purpose**: Show the Monopoly board with all 40 spaces

**Components Needed**:
- Board image background (`monopoly_board.jpg`)
- Player token images (small colored circles/icons)
- Property ownership indicators (colored borders on properties)
- House/hotel icons on properties

**Data to Display**:
```matlab
game = Game.gameInstance();
% Player positions: game.Players(i).position (0-39)
% Property ownership: game.boardSpaces(i).owner
% Houses: game.boardSpaces(i).housesBuilt (for Property objects)
% Hotels: game.boardSpaces(i).hasHotel
```

---

### 2. Game Controls Panel (Right Side)

#### A. Roll Dice Button
- **Label**: "Roll Dice"
- **Enable When**: `game.canRollDice()` returns true
- **Callback**: `game.requestRollDice()`
- **Display**: Show `game.currentDice` after roll

#### B. End Turn Button
- **Label**: "End Turn"
- **Enable When**: It's player's turn and they've completed actions
- **Callback**: `game.requestEndTurn()`

#### C. Property Actions (Show when landing on property)

**Buy Property Button**:
- **Label**: "Buy Property ($XXX)"
- **Enable When**: `game.canBuyProperty()` returns true
- **Callback**:
```matlab
myPlayer = game.getMyPlayer();
game.requestBuyProperty(myPlayer.position);
```

**Decline Property Button**:
- **Label**: "Decline (Auction)"
- **Enable When**: Same as Buy
- **Callback**:
```matlab
myPlayer = game.getMyPlayer();
game.requestDeclineProperty(myPlayer.position);
```

#### D. Building Controls (Show when have monopoly)

**Build House Button**:
- **Label**: "Build House ($XX)"
- **Enable When**: Player owns monopoly and selected property can build
- **Callback**:
```matlab
selectedPosition = % get from UI selection
game.requestBuildHouse(selectedPosition);
```

**Build Hotel Button**:
- **Label**: "Build Hotel ($XX)"
- **Enable When**: Property has 4 houses
- **Callback**:
```matlab
game.requestBuildHotel(selectedPosition);
```

#### E. Mortgage Controls

**Mortgage Button**:
- **Label**: "Mortgage (+$XX)"
- **Enable When**: Property can be mortgaged
- **Callback**:
```matlab
game.requestMortgage(selectedPosition);
```

**Unmortgage Button**:
- **Label**: "Unmortgage (-$XX)"
- **Enable When**: Property is mortgaged
- **Callback**:
```matlab
game.requestUnmortgage(selectedPosition);
```

#### F. Jail Actions (Show when in jail)

**Pay Jail Fine Button**:
- **Label**: "Pay $50"
- **Callback**: `game.requestJailAction('PAY')`

**Use Get Out of Jail Card Button**:
- **Label**: "Use Card"
- **Enable When**: `myPlayer.getOutOfJailCards > 0`
- **Callback**: `game.requestJailAction('CARD')`

**Roll for Doubles Button**:
- **Label**: "Roll for Doubles"
- **Callback**: `game.requestJailAction('ROLL')`

---

### 3. Player Info Panel (Left Side)

**Current Player Indicator**:
- Highlight whose turn it is
- Data: `game.getCurrentPlayer().name`

**My Player Stats**:
```matlab
myPlayer = game.getMyPlayer();
% Display:
% - myPlayer.name
% - myPlayer.money
% - myPlayer.position
% - myPlayer.ownedProperties (list)
% - myPlayer.inJail
```

**Other Players Stats** (repeat for each):
```matlab
for i = 1:length(game.Players)
    player = game.Players(i);
    if ~player.isClient
        % Display player.name, player.money, player.position
    end
end
```

**Monopolies Owned**:
```matlab
monopolies = game.getMyMonopolies();
% Display list of color groups
```

---

### 4. Chat Panel (Bottom)

**Chat Display Area**:
- List/TextArea showing chat history
- Auto-scroll to bottom
- Data:
```matlab
messages = game.getChatMessages();
for i = 1:length(messages)
    msg = messages{i};
    % Display: [msg.playerName]: msg.message
end
```

**Chat Input**:
- Text input field for message
- **Send Button** Callback:
```matlab
messageText = % get from input field
game.sendChatMessage(messageText);
% Clear input field
```

---

### 5. Game Log Panel (Bottom-Right, separate from chat)

**Purpose**: Show game events (not chat)

**Events to Display**:
- "Game started!"
- "Alice rolled [3, 5]"
- "Bob bought Park Place"
- "Charlie built a house on Baltic Avenue"
- "Turn changed to: Alice"
- "Game Over! Winner: Bob"

**Implementation**:
Add event logging in `Game.handleGameEvent()` method (already has fprintf, can also append to a log array).

---

### 6. Property Card Viewer (Modal/Popup)

**Trigger**: Click on any board space

**Display**:
```matlab
position = % clicked position
prop = game.getPropertyAtPosition(position);

% Show:
% - prop.name
% - prop.type
% - prop.purchasePrice
% - prop.rentBase, prop.rent1House, ... prop.rentHotel
% - prop.houseCost, prop.hotelCost
% - prop.owner (if any)
% - prop.isMortgaged
% - prop.housesBuilt, prop.hasHotel
```

---

### 7. Trade Dialog (Modal)

**Trigger**: "Propose Trade" button

**Components**:
- Dropdown to select trading partner
- Checkboxes for my properties to trade
- Number input for my money to give
- Checkboxes for their properties to receive
- Number input for their money to receive
- **Propose Button** Callback:
```matlab
% Build trade offer (not implemented in current version - server needs TradeOfferDTO support)
% This is a future feature
```

---

### 8. Auction Panel (Modal, shows when auction active)

**Display**:
```matlab
% game.auctionInProgress struct
% - propertyPos
% - currentBid
% - highestBidder
```

**Bid Input**:
- Number input for bid amount
- **Place Bid Button** Callback:
```matlab
bidAmount = % get from input
game.requestPlaceBid(bidAmount);
```

---

## Callback Wiring Guide

### Example: Wire Roll Dice Button

In App Designer:
1. Add Button to UI
2. Name it `RollDiceButton`
3. Set ButtonPushedFcn callback:

```matlab
function RollDiceButtonPushed(app, event)
    game = Game.gameInstance();
    if game.canRollDice()
        game.requestRollDice();
    else
        msgbox('Not your turn or cannot roll!', 'Error');
    end
end
```

### Example: Update UI on Game State Change

Add a timer or listener to refresh UI:

```matlab
function updateUI(app)
    game = Game.gameInstance();

    % Update player info
    myPlayer = game.getMyPlayer();
    if ~isempty(myPlayer)
        app.MoneyLabel.Text = sprintf('Money: $%d', myPlayer.money);
        app.PositionLabel.Text = sprintf('Position: %d', myPlayer.position);
    end

    % Update current turn indicator
    app.CurrentTurnLabel.Text = game.getCurrentPlayer().name;

    % Enable/disable buttons
    app.RollDiceButton.Enable = game.canRollDice();
    app.BuyPropertyButton.Enable = game.canBuyProperty();

    % Update chat
    messages = game.getChatMessages();
    chatText = '';
    for i = 1:length(messages)
        msg = messages{i};
        chatText = sprintf('%s[%s]: %s\n', chatText, msg.playerName, msg.message);
    end
    app.ChatTextArea.Value = chatText;
end
```

Call `updateUI(app)` on a timer or after each action.

---

## UI Element Summary

### Buttons to Add:
1. **Roll Dice** → `game.requestRollDice()`
2. **End Turn** → `game.requestEndTurn()`
3. **Buy Property** → `game.requestBuyProperty(position)`
4. **Decline Property** → `game.requestDeclineProperty(position)`
5. **Build House** → `game.requestBuildHouse(position)`
6. **Build Hotel** → `game.requestBuildHotel(position)`
7. **Mortgage** → `game.requestMortgage(position)`
8. **Unmortgage** → `game.requestUnmortgage(position)`
9. **Pay Jail Fine** → `game.requestJailAction('PAY')`
10. **Use Jail Card** → `game.requestJailAction('CARD')`
11. **Roll for Jail** → `game.requestJailAction('ROLL')`
12. **Place Bid** → `game.requestPlaceBid(amount)`
13. **Send Chat** → `game.sendChatMessage(text)`

### Display Elements to Add:
1. **Board** - Visual representation with player positions
2. **Dice Display** - Show `game.currentDice`
3. **Player List** - All players with stats
4. **My Player Panel** - Money, position, properties
5. **Property Card Viewer** - Modal showing property details
6. **Chat Display** - TextArea with chat history
7. **Game Log** - Event history
8. **Current Turn Indicator** - Highlight active player
9. **Monopolies List** - Color groups owned
10. **Property Ownership Indicators** - Colored borders on board

---

## How Game State Updates Work

1. **Player Action**: User clicks button → calls `game.requestXXX()`
2. **WebSocket**: Game.m sends message to server
3. **Server Processing**: Server validates, executes game logic
4. **Broadcast**: Server sends event to all clients
5. **Client Update**: `StompClient` receives → calls `game.processWebsocketMessage()`
6. **State Sync**: `Game.handleGameEvent()` updates local state
7. **UI Refresh**: Your `updateUI()` function reads new state and updates display

**Key Point**: You never directly modify game state. Always request actions via `game.requestXXX()` methods, then update UI based on the synced state.

---

## Example Complete Flow: Rolling Dice

1. **UI**: User clicks "Roll Dice" button
2. **Callback**: `game.requestRollDice()`
3. **WebSocket**: Sends `/app/room/{roomId}/game/roll`
4. **Server**: Rolls dice, moves player, calculates effects
5. **Broadcast**: Sends `DICE_ROLLED` and `PLAYER_MOVED` events
6. **All Clients**: Receive events
7. **Game.m**: `handleGameEvent()` updates `game.currentDice` and `player.position`
8. **UI**: `updateUI()` reads new values and refreshes display

---

## Notes

- **All game logic is server-side** - Client just displays state and sends requests
- **Use Game singleton**: Always access via `game = Game.gameInstance()`
- **Check conditions before enabling buttons**: Use helper methods like `canRollDice()`, `canBuyProperty()`
- **Handle errors gracefully**: Server may reject actions (shown in ERROR events)
- **Chat is separate from game events**: Different message types, different displays

---

## Testing Checklist

- [ ] Can roll dice on my turn
- [ ] Can buy properties
- [ ] Can build houses/hotels on monopolies
- [ ] Can mortgage/unmortgage properties
- [ ] Can send chat messages
- [ ] Jail actions work (pay/card/roll)
- [ ] Turn advances correctly
- [ ] Other players' actions update on my screen
- [ ] Game ends when one player remains
- [ ] All 40 board spaces display correctly

---

## Common Issues & Solutions

**Q: Buttons not enabling**
- Check `game.isMyTurn()` returns true
- Check game phase is 'IN_PROGRESS'
- Verify WebSocket connection is active

**Q: Properties not showing ownership**
- Ensure `handleGameEvent()` processes `PROPERTY_BOUGHT`
- Check `player.addProperty()` is being called
- Verify `prop.owner` is set correctly

**Q: Chat messages not appearing**
- Check `handleChatMessage()` is adding to `chatMessages` array
- Verify UI is reading from `game.getChatMessages()`
- Ensure chat TextArea is updating

**Q: Game state out of sync**
- Verify all game events are handled in `handleGameEvent()`
- Check WebSocket connection is stable
- Ensure `processWebsocketMessage()` is being called for all messages

---

Good luck building the UI! All the game logic is ready and waiting for you to connect it to your App Designer interface. 🎲🏠
