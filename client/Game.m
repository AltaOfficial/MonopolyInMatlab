classdef Game < handle
    % Game - Singleton class managing Monopoly game state and WebSocket communication
    % This class acts as both the board and room management system

    properties
        % Room fields (from server GameRoom)
        roomId                  % UUID string of the room
        roomName                % String name of the room
        isStarted = false       % Boolean - has game started

        % Game fields
        boardSpaces = []        % Array of 40 BoardSpace/Property objects
        gamePlayers = {}        % Array of Player objects (matches server gamePlayers)
        chanceCards             % Array of Card objects
        communityChestCards     % Array of Card objects
        chanceCardIndex = 0     % Current index in chance deck
        communityChestCardIndex = 0  % Current index in community chest deck
        gamePhase = 'LOBBY'     % 'LOBBY', 'IN_PROGRESS', 'FINISHED'
        currentPlayerIndex = 0  % Index of current player
        lastDiceRoll = [0, 0]   % Last dice roll
        doublesCount = 0        % Consecutive doubles rolled
        currentAuction          % Struct: auction data
        currentTrade            % Struct: trade data
        chatHistory             % Cell array of chat message structs
        winnerId                % UUID string of winner
        totalHousesRemaining = 32   % Houses available
        totalHotelsRemaining = 12   % Hotels available

        % Client-only properties
        weAreHost = false       % True if local player is host
        myPlayerId              % UUID of the local player
        currentPlayerTurnId     % UUID of player whose turn it is (derived from currentPlayerIndex)
        stompClient             % Reference to StompClient singleton
        app                     % Reference to the App Designer app

        % Backwards compatibility aliases
        Players                 % Alias for gamePlayers
        currentDice             % Alias for lastDiceRoll
        winner                  % Alias for winnerId (Player object)
        auctionInProgress       % Alias for currentAuction
        pendingTrade            % Alias for currentTrade
        chatMessages            % Alias for chatHistory
    end

    methods (Static)
        function obj = gameInstance(app, roomId, userIsHost, myPlayerId)
            import webRequest.*;
            % Get singleton instance
            % Usage:
            %   game = Game.gameInstance(app, roomId, userIsHost, myPlayerId)  - Initialize with app, roomId, host status, and local player ID
            %   game = Game.gameInstance()     - Get existing instance

            persistent game;

            if nargin > 0
                % New instance requested with app parameter and roomId
                if isempty(game)
                    % Getting the room details from the server
                    roomDetails = [];

                    response = webRequest(sprintf("http://localhost:8000/menu/room/%s", roomId));
                    if ~isempty(response.Body.Data)
                        roomDetails = response.Body.Data;
                    else
                        error("failed to get room details from server");
                    end

                    game = Game(app, roomDetails, userIsHost, myPlayerId);
                else
                    % Instance already exists, update app reference and roomId
                    game.app = app;
                    game.roomId = roomId;
                    game.weAreHost = userIsHost;
                    if nargin > 3
                        game.myPlayerId = myPlayerId;
                    end

                    % Fetch updated room details if roomId changed

                    response = webRequest(sprintf("http://localhost:8000/menu/room/%s", roomId));
                    if ~isempty(response.Body.Data)
                        game.updateFromServerData(response.Body.Data);
                    else
                        error("failed to get room details from server");
                    end
                end
            else
                % Just getting existing instance
                if isempty(game)
                    error('Game instance not initialized. Call Game.gameInstance(app, roomId, userIsHost, myPlayerId) first.');
                end
            end

            obj = game;
        end
    end

    methods (Access = private)
        function obj = Game(app, roomDetails, userIsHost, myPlayerId)
            % Private constructor - use gameInstance(app, roomDetails, userIsHost, myPlayerId) instead
            if nargin > 0
                obj.app = app;
            else
                obj.app = [];
            end

            % Initialize from roomDetails if provided
            if nargin > 1 && ~isempty(roomDetails)
                obj.updateFromServerData(roomDetails);
            else
                obj.gamePlayers = [];
                obj.boardSpaces = [];
                obj.chatHistory = {};
                obj.chanceCards = [];
                obj.communityChestCards = [];
            end

            if nargin > 2
                obj.weAreHost = userIsHost;
                if(userIsHost == true)
                    app.EndTurnButton.Text = "Start Game";
                    app.EndTurnButton.Enable = "on";
                else
                    app.EndTurnButton.Text = "End turn";
                    app.EndTurnButton.Enable = "off";
                end
            end

            if nargin > 3
                obj.myPlayerId = myPlayerId;
            end

            % Set up aliases for backwards compatibility
            obj.Players = obj.gamePlayers;
            obj.currentDice = obj.lastDiceRoll;
            obj.chatMessages = obj.chatHistory;
            obj.auctionInProgress = obj.currentAuction;
            obj.pendingTrade = obj.currentTrade;

            % Note: StompClient will be set later when connection is established
            obj.stompClient = [];
        end
    end

    methods
        %% ===== INITIALIZATION =====

        function setStompClient(obj, stompClient)
            % Set the StompClient reference after connection is established
            obj.stompClient = stompClient;
        end

        function updateFromServerData(obj, gameRoomData)
            % Update Game state from server GameRoom data
            disp(gameRoomData);

            if(nargin < 1)
                gameRoomData = webRequest(sprintf("http://localhost:8000/menu/room/%s", obj.roomId)).Body.Data;
            end

            if isfield(gameRoomData, 'roomId')
                obj.roomId = gameRoomData.roomId;
            end
            if isfield(gameRoomData, 'roomName')
                obj.roomName = gameRoomData.roomName;
                % TODO: Update UI with room name display
            end
            if isfield(gameRoomData, 'isStarted')
                obj.isStarted = gameRoomData.isStarted;
                % TODO: Update UI to show game has started (disable join, enable game controls)
            end
            if isfield(gameRoomData, 'boardSpaces')
                obj.initializeBoard(gameRoomData.boardSpaces);
                % TODO: Update UI to render board spaces
            end
            if isfield(gameRoomData, 'gamePlayers')
                obj.gamePlayers = {};

                playersData = gameRoomData.gamePlayers;

                % Handle both cell array and struct array formats
                if iscell(playersData)
                    numPlayers = length(playersData);
                    for i = 1:numPlayers
                        obj.app.(['Player' num2str(i) 'Label']).Text = playersData{i}.playerName;
                        newPlayer = Player.fromServerData(playersData{i});
                        % Mark if this is the local player
                        if ~isempty(obj.myPlayerId) && strcmp(newPlayer.playerId, obj.myPlayerId)
                            newPlayer.isClient = true;
                            % since this player is the client, we need to
                            % update our app to show its details(money,
                            % houses, etc.)
                            obj.app.YourFunds100Label.Text = sprintf("Your Funds: $%d", newPlayer.money);
                        end
                        obj.gamePlayers{i} = newPlayer;
                    end
                elseif isstruct(playersData)
                    % Handle single struct or struct array
                    numPlayers = length(playersData);
                    for i = 1:numPlayers
                        obj.app.(['Player' num2str(i) 'Label']).Text = playersData(i).playerName;
                        newPlayer = Player.fromServerData(playersData(i));
                        % Mark if this is the local player
                        if ~isempty(obj.myPlayerId) && strcmp(newPlayer.playerId, obj.myPlayerId)
                            newPlayer.isClient = true;
                            % since this player is the client, we need to
                            % update our app to show its details(money,
                            % properites, etc.)
                            newPlayer.ownedPropertyPositions = {1};
                            for i = 1 : length(newPlayer.ownedPropertyPositions)
                                obj.app.propertiesLabel.Text = [obj.app.propertiesLabel.Text, {sprintf("%s", obj.boardSpaces{i}.name)}];
                            end
                            obj.app.YourFunds100Label.Text = sprintf("Your Funds: $%d", newPlayer.money);
                        end
                        obj.gamePlayers{i} = newPlayer;
                    end
                end

                obj.Players = obj.gamePlayers;  % Update alias
                % TODO: Update UI player list display
                obj.app.PlayersConnectedLabel.Text = sprintf("Players Connected(%d): ", length(obj.gamePlayers));
            end
            if isfield(gameRoomData, 'chanceCards')
                obj.chanceCards = gameRoomData.chanceCards;
            end
            if isfield(gameRoomData, 'communityChestCards')
                obj.communityChestCards = gameRoomData.communityChestCards;
            end
            if isfield(gameRoomData, 'chanceCardIndex')
                obj.chanceCardIndex = gameRoomData.chanceCardIndex;
            end
            if isfield(gameRoomData, 'communityChestCardIndex')
                obj.communityChestCardIndex = gameRoomData.communityChestCardIndex;
            end
            if isfield(gameRoomData, 'gamePhase')
                obj.gamePhase = gameRoomData.gamePhase;
                % TODO: Update UI based on game phase (LOBBY/IN_PROGRESS/FINISHED)
            end
            if isfield(gameRoomData, 'currentPlayerIndex')
                obj.currentPlayerIndex = gameRoomData.currentPlayerIndex;
                % Update currentPlayerTurnId from index
                if ~isempty(obj.gamePlayers) && obj.currentPlayerIndex < length(obj.gamePlayers)
                    obj.currentPlayerTurnId = obj.gamePlayers{obj.currentPlayerIndex + 1}.playerId;
                end
                % TODO: Update UI to highlight current player's turn
            end
            if isfield(gameRoomData, 'lastDiceRoll')
                obj.lastDiceRoll = gameRoomData.lastDiceRoll;
                obj.currentDice = obj.lastDiceRoll;  % Update alias
                % TODO: Update UI to display dice roll result
            end
            if isfield(gameRoomData, 'doublesCount')
                obj.doublesCount = gameRoomData.doublesCount;
            end
            if isfield(gameRoomData, 'currentAuction')
                obj.currentAuction = gameRoomData.currentAuction;
                obj.auctionInProgress = obj.currentAuction;  % Update alias
                % TODO: Update UI to show auction panel with property and current bid
            end
            if isfield(gameRoomData, 'currentTrade')
                obj.currentTrade = gameRoomData.currentTrade;
                obj.pendingTrade = obj.currentTrade;  % Update alias
                % TODO: Update UI to show trade offer dialog
            end
            if isfield(gameRoomData, 'chatHistory')
                obj.chatHistory = gameRoomData.chatHistory;
                obj.chatMessages = obj.chatHistory;  % Update alias
                % TODO: Update UI chat display with all messages
            end
            if isfield(gameRoomData, 'winnerId')
                obj.winnerId = gameRoomData.winnerId;
                obj.winner = obj.getPlayerById(obj.winnerId);  % Update alias
                % TODO: Update UI to show winner and end game screen
            end
            if isfield(gameRoomData, 'totalHousesRemaining')
                obj.totalHousesRemaining = gameRoomData.totalHousesRemaining;
                % TODO: Update UI to display houses remaining
            end
            if isfield(gameRoomData, 'totalHotelsRemaining')
                obj.totalHotelsRemaining = gameRoomData.totalHotelsRemaining;
                % TODO: Update UI to display hotels remaining
            end
        end

        %% ===== MESSAGE ROUTING =====

        function processWebsocketMessage(obj, msgJson)
            % Main message router - routes by messageType
            try
                if isfield(msgJson, 'messageType')
                    messageType = msgJson.messageType;
                    disp(messageType);

                    if strcmp(messageType, 'CHAT_MESSAGE')
                        obj.handleChatMessage(msgJson);
                    else
                        % All other message types are game events
                        obj.handleGameEvent(msgJson);
                    end
                end
            catch ME
                fprintf('Error processing WebSocket message: %s\n', ME.message);
            end
        end

        function handleChatMessage(obj, msgJson)
            % Handle incoming chat message
            chatMsg = struct(...
                'playerId', msgJson.playerId, ...
                'playerName', msgJson.playerName, ...
                'message', msgJson.message);

            obj.chatHistory{end+1} = chatMsg;
            obj.chatMessages = obj.chatHistory;  % Update alias

            % Append to TextArea
            obj.appendToTextArea(sprintf('%s: %s', msgJson.playerName, msgJson.message));
        end

        function handleGameEvent(obj, msgJson)
            % Handle incoming game event
            messageType = msgJson.messageType;
            data = msgJson.data;

            % Fetch and update game state from server
            
            response = webRequest(sprintf("http://localhost:8000/menu/room/%s", obj.roomId));
            if ~isempty(response.Body.Data)
                obj.updateFromServerData(response.Body.Data);
            end

            fprintf('[GAME EVENT] %s', messageType);

            switch messageType
                case 'GAME_STARTED'
                    obj.gamePhase = 'IN_PROGRESS';
                    obj.currentPlayerTurnId = data.currentPlayerId;
                    obj.isStarted = true;
                    obj.appendToTextArea(sprintf('Game started! Current player: %s', obj.currentPlayerTurnId));
                    % TODO: Update UI - disable lobby controls, enable game controls, show current player

                case 'DICE_ROLLED'
                    obj.lastDiceRoll = data.dice;
                    obj.currentDice = obj.lastDiceRoll;  % Update alias
                    obj.appendToTextArea(sprintf('Player rolled: [%d, %d]', data.dice(1), data.dice(2)));
                    % TODO: Update UI - animate dice roll, display dice values

                case 'PLAYER_MOVED'
                    playerId = data.playerId;
                    player = obj.getPlayerById(playerId);
                    if ~isempty(player)
                        player.position = data.newPosition;
                        player.money = data.money;
                        obj.appendToTextArea(sprintf('%s moved to position %d', player.playerName, data.newPosition));
                    end
                    % TODO: Update UI - animate player token movement, update player money display

                case 'PROPERTY_BOUGHT'
                    playerId = data.playerId;
                    position = data.position;
                    player = obj.getPlayerById(playerId);
                    if ~isempty(player)
                        prop = obj.boardSpaces{position + 1};  % MATLAB 1-indexing
                        player.addProperty(prop);
                        player.money = data.money;
                        obj.appendToTextArea(sprintf('%s bought %s', player.playerName, prop.name));
                    end
                    % TODO: Update UI - show property ownership color, update player money and properties list

                case 'HOUSE_BUILT'
                    position = data.position;
                    prop = obj.boardSpaces{position + 1};
                    prop.housesBuilt = data.housesBuilt;
                    obj.appendToTextArea(sprintf('House built on %s (total: %d)', prop.name, data.housesBuilt));
                    % TODO: Update UI - display house icons on property

                case 'HOTEL_BUILT'
                    position = data.position;
                    prop = obj.boardSpaces{position + 1};
                    prop.hasHotel = true;
                    prop.housesBuilt = 0;
                    obj.appendToTextArea(sprintf('Hotel built on %s', prop.name));
                    % TODO: Update UI - replace house icons with hotel icon

                case 'PROPERTY_MORTGAGED'
                    position = data.position;
                    prop = obj.boardSpaces{position + 1};
                    prop.isMortgaged = true;
                    obj.appendToTextArea(sprintf('%s mortgaged', prop.name));
                    % TODO: Update UI - show property as mortgaged (grayed out or marked)

                case 'PROPERTY_UNMORTGAGED'
                    position = data.position;
                    prop = obj.boardSpaces{position + 1};
                    prop.isMortgaged = false;
                    obj.appendToTextArea(sprintf('%s unmortgaged', prop.name));
                    % TODO: Update UI - restore property appearance from mortgaged state

                case 'RENT_PAID'
                    obj.appendToTextArea(sprintf('Rent paid: $%d from %s to %s', ...
                        data.amount, data.payerId, data.payeeId));
                    % TODO: Update UI - show transaction animation, update both players' money

                case 'CARD_DRAWN'
                    obj.appendToTextArea(sprintf('Card drawn: %s', data.description));
                    % TODO: Update UI - display card popup with description and action

                case 'TURN_CHANGED'
                    obj.currentPlayerTurnId = data.currentPlayerId;
                    obj.doublesCount = 0;
                    obj.appendToTextArea(sprintf('Turn changed to: %s', obj.currentPlayerTurnId));
                    % TODO: Update UI - highlight current player, enable/disable controls based on turn

                case 'GAME_OVER'
                    obj.gamePhase = 'FINISHED';
                    obj.winner = obj.getPlayerById(data.winnerId);
                    obj.appendToTextArea(sprintf('Game over! Winner: %s', obj.winner.playerName));
                    % TODO: Update UI - show winner screen with confetti/celebration

                case 'PLAYER_JOINED'
                    obj.appendToTextArea(sprintf('Player joined: %s', data.playerName));
                    % TODO: Update UI - add player to player list in lobby

                case 'TRADE_PROPOSED'
                    obj.appendToTextArea('Trade proposed');
                    % TODO: Update UI - show trade offer dialog to recipient

                case 'TRADE_COMPLETED'
                    obj.appendToTextArea('Trade completed');
                    % TODO: Update UI - close trade dialog, update both players' properties and money

                case 'TRADE_DECLINED'
                    obj.appendToTextArea('Trade declined');
                    % TODO: Update UI - close trade dialog, notify trade was declined

                case 'AUCTION_STARTED'
                    obj.appendToTextArea(sprintf('Auction started for position %d', data.position));
                    % TODO: Update UI - show auction panel with property details and bid controls

                case 'BID_PLACED'
                    obj.appendToTextArea(sprintf('Bid placed: $%d', data.amount));
                    % TODO: Update UI - update auction panel with new highest bid and bidder

                case 'AUCTION_ENDED'
                    obj.appendToTextArea('Auction ended');
                    % TODO: Update UI - close auction panel

                case 'PLAYER_RELEASED_JAIL'
                    playerId = data.playerId;
                    player = obj.getPlayerById(playerId);
                    if ~isempty(player)
                        player.inJail = ~data.released;
                        if data.released
                            obj.appendToTextArea(sprintf('%s was released from jail', player.playerName));
                        else
                            obj.appendToTextArea(sprintf('%s is still in jail', player.playerName));
                        end
                    end
                    % TODO: Update UI - update jail status indicator for player

                case 'ERROR'
                    obj.appendToTextArea(sprintf('[ERROR] %s', data.error));
                    % TODO: Update UI - display error dialog or notification to user

                otherwise
                    obj.appendToTextArea(sprintf('Unhandled game event: %s', messageType));
            end
        end

        %% ===== CHAT METHODS =====

        function sendChatMessage(obj, message)
            % Send chat message to server
            if isempty(obj.stompClient)
                warning('StompClient not connected');
                return;
            end

            fprintf("sending message: %s", message);

            myPlayer = obj.getMyPlayer();
            payload = struct(...
                'playerId', myPlayer.playerId, ...
                'playerName', myPlayer.playerName, ...
                'message', message);

            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/chat', obj.roomId), payload);
        end

        function messages = getChatMessages(obj)
            % Get all chat messages
            messages = obj.chatHistory;
        end

        %% ===== GAME ACTION REQUESTS (Client → Server) =====

        function requestStartGame(obj)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/start', obj.roomId), struct());
        end

        function requestRollDice(obj)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/roll', obj.roomId), payload);
        end

        function requestBuyProperty(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'position', position);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/buyProperty', obj.roomId), payload);
        end

        function requestDeclineProperty(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'position', position);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/declineProperty', obj.roomId), payload);
        end

        function requestBuildHouse(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'position', position);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/buildHouse', obj.roomId), payload);
        end

        function requestBuildHotel(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'position', position);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/buildHotel', obj.roomId), payload);
        end

        function requestMortgage(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'position', position);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/mortgage', obj.roomId), payload);
        end

        function requestUnmortgage(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'position', position);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/unmortgage', obj.roomId), payload);
        end

        function requestJailAction(obj, actionType)
            % actionType: 'PAY', 'CARD', or 'ROLL'
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'action', actionType);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/jailAction', obj.roomId), payload);
        end

        function requestEndTurn(obj)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/endTurn', obj.roomId), payload);
        end

        function requestPlaceBid(obj, amount)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'amount', amount);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/placeBid', obj.roomId), payload);
        end

        %% ===== HELPER METHODS =====

        function player = getCurrentPlayer(obj)
            % Get player whose turn it is
            player = obj.getPlayerById(obj.currentPlayerTurnId);
        end

        function player = getMyPlayer(obj)
            % Get local player (isClient = true)
            for i = 1:length(obj.gamePlayers)
                if obj.gamePlayers{i}.isClient
                    player = obj.gamePlayers{i};
                    return;
                end
            end
            player = [];
        end

        function player = getPlayerById(obj, playerId)
            % Find player by ID
            for i = 1:length(obj.gamePlayers)
                if strcmp(obj.gamePlayers{i}.playerId, playerId)
                    player = obj.gamePlayers{i};
                    return;
                end
            end
            player = [];
        end

        function isTurn = isMyTurn(obj)
            % Check if it's local player's turn
            myPlayer = obj.getMyPlayer();
            if isempty(myPlayer)
                isTurn = false;
            else
                isTurn = strcmp(obj.currentPlayerTurnId, myPlayer.playerId);
            end
        end

        function can = canRollDice(obj)
            % Check if can roll dice
            can = obj.isMyTurn() && strcmp(obj.gamePhase, 'IN_PROGRESS');
        end

        function can = canBuyProperty(obj)
            % Check if can buy property at current position
            myPlayer = obj.getMyPlayer();
            if isempty(myPlayer) || ~obj.isMyTurn()
                can = false;
                return;
            end

            prop = obj.boardSpaces{myPlayer.position + 1};  % MATLAB 1-indexing
            can = isempty(prop.owner) && myPlayer.canAfford(prop.purchasePrice);
        end

        function monopolies = getMyMonopolies(obj)
            % Get color groups where I have monopoly
            myPlayer = obj.getMyPlayer();
            if isempty(myPlayer)
                monopolies = {};
            else
                monopolies = myPlayer.monopolies;
            end
        end

        function props = getMyProperties(obj)
            % Get my owned properties
            myPlayer = obj.getMyPlayer();
            if isempty(myPlayer)
                props = [];
            else
                props = myPlayer.ownedProperties;
            end
        end

        function prop = getPropertyAtPosition(obj, position)
            % Get property at board position (0-indexed)
            if position >= 0 && position < 40
                prop = obj.boardSpaces{position + 1};  % MATLAB 1-indexing
            else
                prop = [];
            end
        end

        function appendToTextArea(obj, text)
            % Append text to TextArea with proper newline handling
            % Top = oldest, Bottom = newest
            currentValue = obj.app.TextArea.Value;
            if isempty(currentValue) || (iscell(currentValue) && isempty(currentValue{1}))
                % First item, no newline needed
                obj.app.TextArea.Value = {text};
            else
                % Append with newline
                obj.app.TextArea.Value = [currentValue; {text}];
            end
        end

        function players = getPlayersByPosition(obj, position)
            % Get all players at a specific position
            players = {};
            for i = 1:length(obj.gamePlayers)
                if obj.gamePlayers{i}.position == position
                    players{end+1} = obj.gamePlayers{i};
                end
            end
        end

        function initializeBoard(obj, boardData)
            % Initialize board from server data
            % boardSpaces will be a cell array containing mixed Property and BoardSpace objects
            obj.boardSpaces = cell(length(boardData), 1);

            for i = 1:length(boardData)
                % Handle both cell array and struct array formats
                if iscell(boardData)
                    spaceData = boardData{i};
                else
                    spaceData = boardData(i);
                end

                if strcmp(spaceData.spaceType, 'PROPERTY') || ...
                   strcmp(spaceData.spaceType, 'RAILROAD') || ...
                   strcmp(spaceData.spaceType, 'UTILITY')
                    obj.boardSpaces{i} = Property.fromServerData(spaceData);
                else
                    obj.boardSpaces{i} = BoardSpace.fromServerData(spaceData);
                end
            end
        end
    end
end
