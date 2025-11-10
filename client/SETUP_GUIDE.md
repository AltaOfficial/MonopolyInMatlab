# Monopoly Game Setup Guide

## Quick Start

### 1. Connect to WebSocket Server

In your MATLAB code (e.g., in `play.m` or when creating a room):

```matlab
% Connect to WebSocket server
wsUrl = 'ws://localhost:8080/monopoly';  % Adjust URL as needed
stompClient = StompClient.getInstance(wsUrl);

% Set the StompClient reference in Game singleton
game = Game.gameInstance();
game.setStompClient(stompClient);
```

### 2. Subscribe to Room

When joining a room:

```matlab
% Set room ID in game
game.RoomId = 'your-room-uuid-here';

% Subscribe to room topic
stompClient.stompSubscribe(sprintf('/topic/room/%s', game.RoomId));
```

### 3. That's It!

The connection is now complete. All WebSocket messages will automatically:
1. Arrive at `StompClient.onTextMessage()`
2. Get parsed to extract JSON
3. Route to `Game.processWebsocketMessage()`
4. Update game state accordingly

## How It Works

### Message Flow

```
Server Broadcast
      ↓
WebSocket Connection
      ↓
StompClient.onTextMessage() ← parses STOMP frame
      ↓
Game.processWebsocketMessage() ← routes by messageType
      ↓
Game.handleGameEvent() or Game.handleChatMessage()
      ↓
Local game state updated
```

### Example Message Processing

When a player rolls dice:

1. **You click "Roll Dice"** → `game.requestRollDice()`
2. **Sends WebSocket message** via `stompClient.stompSend()`
3. **Server processes** and broadcasts `DICE_ROLLED` event
4. **All clients receive** STOMP MESSAGE frame:
   ```
   MESSAGE
   destination:/topic/room/123
   content-type:application/json

   {"messageType":"DICE_ROLLED","data":{"dice":[3,5],...}}
   ```
5. **StompClient parses** and extracts JSON
6. **Game.processWebsocketMessage()** routes to `handleGameEvent()`
7. **Game state updates** with `game.currentDice = [3, 5]`
8. **UI refreshes** to show new dice values

## Complete Example

Here's a complete example of setting up a game:

```matlab
%% 1. Connect to server
wsUrl = 'ws://localhost:8080/monopoly';
stompClient = StompClient.getInstance(wsUrl);

%% 2. Get game singleton and set client
game = Game.gameInstance();
game.setStompClient(stompClient);

%% 3. Create/join room (your existing room logic)
% Assume you get roomId from server response
roomId = 'abc-123-def-456';
game.RoomId = roomId;

%% 4. Subscribe to room
stompClient.stompSubscribe(sprintf('/topic/room/%s', roomId));

%% 5. Create local player
myPlayer = Player('my-player-uuid', 'Alice', true);
game.Players = [myPlayer];

%% 6. Now you can use all game methods!
% Examples:
game.sendChatMessage('Hello everyone!');
game.requestStartGame();  % If you're the host
game.requestRollDice();   % On your turn
```

## Cleanup

When disconnecting:

```matlab
% Disconnect from server
stompClient.stompDisconnect();
stompClient.close();

% Clear the instance
StompClient.getInstance([]);
```

## Debugging

### Enable Debug Output

The `onTextMessage` already prints all messages:
```matlab
fprintf('Message received:\n%s\n', message);
```

You can add more debug output in `Game.handleGameEvent()`:
```matlab
fprintf('[DEBUG] Event: %s, Data: %s\n', messageType, jsonencode(data));
```

### Check Connection Status

```matlab
stompClient = StompClient.getInstance(wsUrl);
if stompClient.Status == 1
    disp('Connected!');
else
    disp('Not connected');
end
```

### Verify Game State

```matlab
game = Game.gameInstance();

% Check if StompClient is set
if isempty(game.stompClient)
    warning('StompClient not connected to Game!');
end

% Check current game state
disp(['Game Phase: ' game.gamePhase]);
disp(['Room ID: ' game.RoomId]);
disp(['Number of players: ' num2str(length(game.Players))]);
```

## Common Issues

### Issue: "StompClient not connected" warning

**Solution**: Call `game.setStompClient(stompClient)` after creating the connection.

```matlab
stompClient = StompClient.getInstance(wsUrl);
game = Game.gameInstance();
game.setStompClient(stompClient);  % Don't forget this!
```

### Issue: Messages not being processed

**Solution**: Make sure you subscribed to the room topic:
```matlab
stompClient.stompSubscribe(sprintf('/topic/room/%s', game.RoomId));
```

### Issue: Can't send game actions

**Solution**: Ensure:
1. StompClient is set: `game.setStompClient(stompClient)`
2. Room ID is set: `game.RoomId = 'your-room-id'`
3. Local player exists: `game.getMyPlayer()` returns a player

## Integration with Existing Code

If you already have connection logic in `play.m` or elsewhere:

```matlab
% Your existing code
function play()
    % ... existing setup ...

    % Add these lines after WebSocket connection is established
    game = Game.gameInstance();
    stompClient = StompClient.getInstance(wsUrl);  % Use your existing wsUrl
    game.setStompClient(stompClient);

    % ... rest of your code ...
end
```

## Testing the Connection

Quick test to verify everything works:

```matlab
%% Test Script
% 1. Connect
wsUrl = 'ws://localhost:8080/monopoly';
stompClient = StompClient.getInstance(wsUrl);
game = Game.gameInstance();
game.setStompClient(stompClient);

% 2. Set up room (use test room ID)
game.RoomId = 'test-room-123';
stompClient.stompSubscribe('/topic/room/test-room-123');

% 3. Create test player
myPlayer = Player('test-uuid', 'TestPlayer', true);
game.Players = [myPlayer];

% 4. Try sending a chat message
game.sendChatMessage('Test message');

% If you see the message formatted and sent, it's working!
```

---

That's it! The WebSocket integration is now complete. All game actions will automatically sync across all connected clients. 🎮
