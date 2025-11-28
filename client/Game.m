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
            elseif isempty(game)
                    error('Game instance not initialized. Call Game.gameInstance(app, roomId, userIsHost, myPlayerId) first.');
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
            % Create reverse reference so StompClient can access the
            % GameInstance
            stompClient.setGameInstance(obj);
        end

        function updateFromServerData(obj, gameRoomData)
            % Update Game state from server GameRoom data

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
                playersData = gameRoomData.gamePlayers;

                % Check if playersData is empty
                if isempty(playersData)
                    obj.gamePlayers = {};
                    obj.Players = obj.gamePlayers;
                    obj.app.PlayersConnectedLabel.Text = "Players Connected(0): ";
                    return;
                end

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
                        obj.gamePlayers{end+1} = newPlayer;
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
                            propertiesText = "";
                            for j = 1 : length(newPlayer.ownedPropertyPositions)
                                % ownedPropertyPositions is a regular array (use parentheses)
                                % boardSpaces is a cell array (use curly braces)
                                % +1 for MATLAB 1-indexing
                                propertiesText = sprintf("%s\n%s", propertiesText, obj.boardSpaces{newPlayer.ownedPropertyPositions(j) + 1}.name);
                            end
                            obj.app.propertiesLabel.Text = propertiesText;
                            obj.app.YourFunds100Label.Text = sprintf("Your Funds: $%d", newPlayer.money);
                        end
                        fprintf("new player id: %s\n", newPlayer.playerId);
                        disp(obj.gamePlayers);
                        oldPlayer = obj.getPlayerById(newPlayer.playerId);
                        fprintf("----\n\n");
                        disp(oldPlayer);
                        fprintf("----\n\n");
                        if(~isempty(oldPlayer))
                            playerIndex = obj.getPlayerIndexById(newPlayer.playerId);
                            if(obj.isStarted == true)
                                % update player position only when game has
                                % already started
                                switch(playerIndex)
                                    % updating position
                                    case 1 % player is dog
                                        obj.app.(['dog_' num2str(oldPlayer.position)]).Visible = "off";
                                        obj.app.dog_jail_10.Visible = "off";
                                        if(newPlayer.position == 10 && newPlayer.inJail == true)
                                            obj.app.dog_jail_10.Visible = "on";
                                        else
                                            obj.app.(['dog_' num2str(newPlayer.position)]).Visible = "on";
                                        end
                                    case 2 % player is battleship
                                        obj.app.(['battleship_' num2str(oldPlayer.position)]).Visible = "off";
                                        obj.app.battleship_jail_10.Visible = "off";
                                        if(newPlayer.position == 10 && newPlayer.inJail == true)
                                            obj.app.battleship_jail_10.Visible = "on";
                                        else
                                            obj.app.(['battleship_' num2str(newPlayer.position)]).Visible = "on";
                                        end
                                    case 3 % player is hat
                                        obj.app.(['hat_' num2str(oldPlayer.position)]).Visible = "off";
                                        obj.app.hat_jail_10.Visible = "off";
                                        if(newPlayer.position == 10 && newPlayer.inJail == true)
                                            obj.app.hat_jail_10.Visible = "on";
                                        else
                                            obj.app.(['hat_' num2str(newPlayer.position)]).Visible = "on";
                                        end
                                    case 4 % player is car
                                        obj.app.(['car_' num2str(oldPlayer.position)]).Visible = "off";
                                        obj.app.car_jail_10.Visible = "off";
                                        if(newPlayer.position == 10 && newPlayer.inJail == true)
                                            obj.app.car_jail_10.Visible = "on";
                                        else
                                            obj.app.(['car_' num2str(newPlayer.position)]).Visible = "on";
                                        end
                                end
                            end
                            % update old player with new
                            obj.gamePlayers{playerIndex} = newPlayer;
                        else
                            disp("old player is empty");
                            obj.gamePlayers{end+1} = newPlayer;
                        end
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
        end

        function handleChatMessage(obj, msgJson)
            % Handle incoming chat message
            chatMsg = struct(...
                'playerId', msgJson.playerId, ...
                'playerName', msgJson.playerName, ...
                'message', msgJson.message);

            % Ensure chatHistory is a cell array
            if isempty(obj.chatHistory) || ~iscell(obj.chatHistory)
                obj.chatHistory = {};
            end

            obj.chatHistory{end+1} = chatMsg;
            obj.chatMessages = obj.chatHistory;  % Update alias

            % Append to TextArea
            obj.appendToTextArea(sprintf('%s: %s', msgJson.playerName, msgJson.message));

            % scroll textarea to the bottom
            scroll(obj.app.TextArea, "bottom");
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

            % perform actions on updated data

            fprintf('[GAME EVENT] %s', messageType);

            switch messageType
                case 'GAME_STARTED'
                    obj.gamePhase = 'IN_PROGRESS';
                    obj.currentPlayerTurnId = data.currentPlayerId;
                    obj.isStarted = true;
                    obj.appendToTextArea(sprintf('Host has started the game! no more players can connect, and if someone leaves the game ends'));

                    if(obj.weAreHost == true) % host always goes first
                        obj.app.EndTurnButton.Text = "Roll";
                        obj.app.EndTurnButton.Enable = "on";
                    end

                    for i=1 : length(obj.gamePlayers)
                        switch(i)
                            case 1
                                obj.app.dog_0.Visible = "on";
                            case 2
                                obj.app.battleship_0.Visible = "on";
                            case 3
                                obj.app.hat_0.Visible = "on";
                            case 4
                                obj.app.car_0.Visible = "on";
                        end
                    end

                    obj.app.CurrentTurnGamenotstartedLabel.Text = sprintf("Current Turn: %s", obj.getCurrentPlayer().playerName);
                    
                    % TODO: Update UI - disable lobby controls, enable game controls, show current player

                case 'DICE_ROLLED'
                    if(obj.isMyTurn() == true)
                        obj.app.EndTurnButton.Text = "End turn";
                        obj.app.EndTurnButton.Enable = "on";
                    end
                    obj.lastDiceRoll = data.dice;
                    obj.currentDice = obj.lastDiceRoll;  % Update alias
                    obj.appendToTextArea(sprintf('Player rolled: [%d, %d]', data.dice(1), data.dice(2)));
                    % TODO: Update UI - animate dice roll, display dice values

                case 'PLAYER_MOVED'
                    playerId = data.playerId;
                    player = obj.getPlayerById(playerId);
                    if(obj.isMyTurn() == true)
                        obj.app.CurrentSpotOptionsLabel.Enable = "on";
                        propertyAtPlayerPosition = obj.getPropertyAtPosition(player.position);
                        if(obj.canBuyProperty() && (propertyAtPlayerPosition.type == "PROPERTY" || propertyAtPlayerPosition.type == "RAILROAD" || propertyAtPlayerPosition.type == "UTILITY"))
                            obj.app.BuyPropertyHereButton.Enable = "on";
                        end
                        if(propertyAtPlayerPosition.type == "CHANCE" || propertyAtPlayerPosition.type == "COMMUNITY_CHEST")
                            obj.app.EndTurnButton.Text = "Draw Card";
                        end

                    end
                    if ~isempty(player)
                        player.position = data.newPosition;
                        player.money = data.money;
                        obj.appendToTextArea(sprintf('%s moved to %s', player.playerName, obj.getPropertyAtPosition(player.position).name));
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

                case 'CARD_DRAWN'
                    obj.appendToTextArea(sprintf('Card drawn: %s', data.description));
                    if(obj.isMyTurn() == true)
                        obj.app.EndTurnButton.Text = "End turn";
                        obj.app.EndTurnButton.Enable = "on";
                        obj.app.card_drawn_panel.Visible = "on";

                        % Display the appropriate card image based on description
                        description = data.description;
                        switch description
                            % ===== CHANCE CARDS =====
                            case "Go to Jail"
                                if strcmp(data.cardType, 'CHANCE')
                                    obj.app.card_image.ImageSource = 'Chance1.png';
                                else
                                    obj.app.card_image.ImageSource = 'CommunityChest11.png';
                                end
                            case "Advance to St. Charles Place"
                                obj.app.card_image.ImageSource = 'Chance2.png';
                            case "Advance token to nearest Railroad"
                                obj.app.card_image.ImageSource = 'Chance3.png';
                            case "Take a trip to Reading Railroad"
                                obj.app.card_image.ImageSource = 'Chance4.png';
                            case "Your building loan matures (Collect $150)"
                                obj.app.card_image.ImageSource = 'Chance5.png';
                            case "Make general repairs on all your property ($25 per house, $100 per hotel)"
                                obj.app.card_image.ImageSource = 'Chance6.png';
                            case "You have been elected Chairman of the Board (Pay each player $50)"
                                obj.app.card_image.ImageSource = 'Chance8.png';
                            case "Advance to Go (Collect $200)"
                                if strcmp(data.cardType, 'CHANCE')
                                    obj.app.card_image.ImageSource = 'Chance9.png';
                                else
                                    obj.app.card_image.ImageSource = 'CommunityChest16.png';
                                end
                            case "Go Back 3 Spaces"
                                obj.app.card_image.ImageSource = 'Chance10.png';
                            case "Bank pays you dividend of $50"
                                obj.app.card_image.ImageSource = 'Chance11.png';
                            case "Advance to Illinois Avenue"
                                obj.app.card_image.ImageSource = 'Chance12.png';
                            case "Advance token to nearest Utility"
                                obj.app.card_image.ImageSource = 'Chance13.png';
                            case "Take a walk on the Boardwalk"
                                obj.app.card_image.ImageSource = 'Chance14.png';
                            case "Pay poor tax of $15"
                                obj.app.card_image.ImageSource = 'Chance15.png';
                            case "Get Out of Jail Free"
                                if strcmp(data.cardType, 'CHANCE')
                                    obj.app.card_image.ImageSource = 'Chance16.png';
                                else
                                    obj.app.card_image.ImageSource = 'CommunityChest7.png';
                                end

                            % ===== COMMUNITY CHEST CARDS =====
                            case "Bank error in your favor (Collect $200)"
                                obj.app.card_image.ImageSource = 'CommunityChest1.png';
                            case "Doctor's fees (Pay $50)"
                                obj.app.card_image.ImageSource = 'CommunityChest2.png';
                            case "From sale of stock you get $50"
                                obj.app.card_image.ImageSource = 'CommunityChest10.png';
                            case "Grand Opera Night (Collect $50 from every player)"
                                obj.app.card_image.ImageSource = 'CommunityChest3.png';
                            case "Holiday Fund matures (Collect $100)"
                                obj.app.card_image.ImageSource = 'CommunityChest4.png';
                            case "Income tax refund (Collect $20)"
                                obj.app.card_image.ImageSource = 'CommunityChest5.png';
                            case "It is your birthday (Collect $10 from every player)"
                                obj.app.card_image.ImageSource = 'CommunityChest8.png';
                            case "Life insurance matures (Collect $100)"
                                obj.app.card_image.ImageSource = 'CommunityChest12.png';
                            case "Hospital fees (Pay $100)"
                                obj.app.card_image.ImageSource = 'CommunityChest14.png';
                            case "School fees (Pay $150)"
                                obj.app.card_image.ImageSource = 'CommunityChest6.png';
                            case "Receive $25 consultancy fee"
                                obj.app.card_image.ImageSource = 'CommunityChest6.png';
                            case "You are assessed for street repairs ($40 per house, $115 per hotel)"
                                obj.app.card_image.ImageSource = 'CommunityChest9.png';
                            case "You have won second prize in a beauty contest (Collect $10)"
                                obj.app.card_image.ImageSource = 'CommunityChest15.png';
                            case "You inherit $100"
                                obj.app.card_image.ImageSource = 'CommunityChest13.png';
                        end
                    end

                case 'TURN_CHANGED'
                    obj.currentPlayerTurnId = data.currentPlayerId;
                    obj.doublesCount = 0;
                    if(obj.isMyTurn() == true)
                        obj.app.EndTurnButton.Text = "Roll";
                        obj.app.EndTurnButton.Enable = "on";

                        obj.app.OptionsLabel.Enable = "on";
                        obj.app.BuyahouseButton.Enable = "on";
                        obj.app.BuyahotelButton.Enable = "on";
                        obj.app.SellAPropertyButton.Enable = "on";
                        if(obj.getMyPlayer().money >= 50)
                            obj.app.SellAPropertyButton.Enable = "on";
                        end
                        if((obj.getMyPlayer().inJail == true) && (obj.getMyPlayer().getOutOfJailCards > 0))
                            obj.app.UseGetoutofjailfreecardButton.Enable = "on";
                        end
                    else
                        obj.app.EndTurnButton.Enable = "off";

                        obj.app.OptionsLabel.Enable = "off";
                        obj.app.BuyahouseButton.Enable = "off";
                        obj.app.BuyahotelButton.Enable = "off";
                        obj.app.CurrentSpotOptionsLabel.Enable = "off";
                        obj.app.BuyPropertyHereButton.Enable = "off";
                        obj.app.UseGetoutofjailfreecardButton.Enable = "off";
                        obj.app.SellAPropertyButton.Enable = "off";
                    end
                    obj.app.CurrentTurnGamenotstartedLabel.Text = sprintf("Current Turn: %s", obj.getCurrentPlayer().playerName);
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

                case 'PLAYER_GOING_BANKRUPT'
                    if obj.isMyTurn() == true
                        
                    end

                case 'ERROR'
                    obj.appendToTextArea(sprintf('[ERROR] %s', data.error));
                    % TODO: Update UI - display error dialog or notification to user

                otherwise
                    obj.appendToTextArea(sprintf('Unhandled game event: %s', messageType));
            end
            % scroll textarea to the bottom
            scroll(obj.app.TextArea, "bottom");
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

        function requestDrawCard(obj, deckType)
            % deckType: 'COMMUNITY' or 'CHANCE'
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerId, 'deckType', deckType);
            obj.stompClient.stompSend(sprintf('/monopoly/room/%s/game/drawCard', obj.roomId), payload);
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
            % If not found by isClient flag, try matching by playerId
            if ~isempty(obj.myPlayerId)
                for i = 1:length(obj.gamePlayers)
                    if strcmp(obj.gamePlayers{i}.playerId, obj.myPlayerId)
                        obj.gamePlayers{i}.isClient = true; % Mark it for future calls
                        player = obj.gamePlayers{i};
                        return;
                    end
                end
            end
            fprintf('Warning: Could not find local player. myPlayerId=%s, numPlayers=%d\n', ...
                string(obj.myPlayerId), length(obj.gamePlayers));
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

        function playerIndex = getPlayerIndexById(obj, playerId)
            % Find playerIndex by ID
            for i = 1:length(obj.gamePlayers)
                
                if strcmp(obj.gamePlayers{i}.playerId, playerId)
                    playerIndex = i;
                    return;
                end
            end
            playerIndex = [];
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

            space = obj.boardSpaces{myPlayer.position + 1};  % MATLAB 1-indexing
            % Check if this is actually a Property (not a BoardSpace like GO, Jail, etc.)
            if isa(space, 'Property')
                can = isempty(space.owner) && myPlayer.canAfford(space.purchasePrice);
            else
                can = false;
            end
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
