classdef Game < handle
    % Game - Singleton class managing Monopoly game state and WebSocket communication
    % This class acts as both the board and room management system

    properties
        RoomId                  % UUID string of the room
        Players                 % Array of Player objects (all players in room)
        boardSpaces             % Array of 40 BoardSpace/Property objects
        weAreHost = false       % True if local player is host
        currentPlayerTurnId     % UUID of player whose turn it is

        % Game state
        gamePhase = 'LOBBY'     % 'LOBBY', 'IN_PROGRESS', 'FINISHED'
        currentDice = [0, 0]    % Last dice roll
        doublesCount = 0        % Consecutive doubles rolled
        winner                  % Player object who won

        % Card decks (metadata only - server manages shuffling)
        chanceCards             % Array of Card objects
        communityChestCards     % Array of Card objects

        % Current actions
        auctionInProgress       % Struct: {propertyPos, currentBid, highestBidder}
        pendingTrade            % Struct: {from, to, offer}

        % Chat
        chatMessages            % Cell array of chat message structs

        % WebSocket client reference
        stompClient             % Reference to StompClient singleton

        % UI App reference (for updating UI elements)
        app                     % Reference to the App Designer app
    end

    methods (Static)
        function obj = gameInstance(app)
            % Get singleton instance
            % Usage:
            %   game = Game.gameInstance(app)  - Initialize with app reference
            %   game = Game.gameInstance()     - Get existing instance

            persistent game;

            if nargin > 0
                % New instance requested with app parameter
                if isempty(game)
                    game = Game(app);
                else
                    % Instance already exists, update app reference
                    game.app = app;
                end
            else
                % Just getting existing instance
                if isempty(game)
                    error('Game instance not initialized. Call Game.gameInstance(app) first with your app parameter.');
                end
            end

            obj = game;
        end
    end

    methods (Access = private)
        function obj = Game(app)
            % Private constructor - use gameInstance(app) instead
            if nargin > 0
                obj.app = app;
            else
                obj.app = [];
            end

            obj.Players = [];
            obj.boardSpaces = [];
            obj.chatMessages = {};
            obj.chanceCards = [];
            obj.communityChestCards = [];
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

        %% ===== MESSAGE ROUTING =====

        function processWebsocketMessage(obj, msgJson)
            % Main message router - routes by messageType
            try
                if isfield(msgJson, 'messageType')
                    messageType = msgJson.messageType;

                    if strcmp(messageType, 'CHAT_MESSAGE')
                        obj.handleChatMessage(msgJson);
                    elseif startsWith(messageType, 'GAME_') || strcmp(messageType, 'ERROR')
                        obj.handleGameEvent(msgJson);
                    else
                        warning('Unknown message type: %s', messageType);
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
                'message', msgJson.message, ...
                'timestamp', msgJson.timestamp);

            obj.chatMessages{end+1} = chatMsg;

            % TODO: Trigger UI update callback here
            fprintf('[CHAT] %s: %s\n', msgJson.playerName, msgJson.message);
        end

        function handleGameEvent(obj, msgJson)
            % Handle incoming game event
            messageType = msgJson.messageType;
            data = msgJson.data;

            fprintf('[GAME EVENT] %s\n', messageType);

            switch messageType
                case 'GAME_STARTED'
                    obj.gamePhase = 'IN_PROGRESS';
                    obj.currentPlayerTurnId = data.currentPlayerId;
                    fprintf('Game started! Current player: %s\n', obj.currentPlayerTurnId);

                case 'DICE_ROLLED'
                    obj.currentDice = data.dice;
                    fprintf('Player rolled: [%d, %d]\n', data.dice(1), data.dice(2));

                case 'PLAYER_MOVED'
                    playerId = data.playerId;
                    player = obj.getPlayerById(playerId);
                    if ~isempty(player)
                        player.position = data.newPosition;
                        player.money = data.money;
                        fprintf('%s moved to position %d\n', player.name, data.newPosition);
                    end

                case 'PROPERTY_BOUGHT'
                    playerId = data.playerId;
                    position = data.position;
                    player = obj.getPlayerById(playerId);
                    if ~isempty(player)
                        prop = obj.boardSpaces(position + 1);  % MATLAB 1-indexing
                        player.addProperty(prop);
                        player.money = data.money;
                        fprintf('%s bought %s\n', player.name, prop.name);
                    end

                case 'HOUSE_BUILT'
                    position = data.position;
                    prop = obj.boardSpaces(position + 1);
                    prop.housesBuilt = data.housesBuilt;
                    fprintf('House built on %s (total: %d)\n', prop.name, data.housesBuilt);

                case 'HOTEL_BUILT'
                    position = data.position;
                    prop = obj.boardSpaces(position + 1);
                    prop.hasHotel = true;
                    prop.housesBuilt = 0;
                    fprintf('Hotel built on %s\n', prop.name);

                case 'PROPERTY_MORTGAGED'
                    position = data.position;
                    prop = obj.boardSpaces(position + 1);
                    prop.isMortgaged = true;
                    fprintf('%s mortgaged\n', prop.name);

                case 'PROPERTY_UNMORTGAGED'
                    position = data.position;
                    prop = obj.boardSpaces(position + 1);
                    prop.isMortgaged = false;
                    fprintf('%s unmortgaged\n', prop.name);

                case 'RENT_PAID'
                    fprintf('Rent paid: $%d from %s to %s\n', ...
                        data.amount, data.payerId, data.payeeId);

                case 'CARD_DRAWN'
                    fprintf('Card drawn: %s\n', data.description);

                case 'TURN_CHANGED'
                    obj.currentPlayerTurnId = data.currentPlayerId;
                    obj.doublesCount = 0;
                    fprintf('Turn changed to: %s\n', obj.currentPlayerTurnId);

                case 'GAME_OVER'
                    obj.gamePhase = 'FINISHED';
                    obj.winner = obj.getPlayerById(data.winnerId);
                    fprintf('Game over! Winner: %s\n', obj.winner.name);

                case 'TRADE_PROPOSED'
                    fprintf('Trade proposed\n');

                case 'AUCTION_STARTED'
                    fprintf('Auction started for position %d\n', data.position);

                case 'BID_PLACED'
                    fprintf('Bid placed: $%d\n', data.amount);

                case 'ERROR'
                    fprintf('[ERROR] %s\n', data.error);

                otherwise
                    fprintf('Unhandled game event: %s\n', messageType);
            end
        end

        %% ===== CHAT METHODS =====

        function sendChatMessage(obj, message)
            % Send chat message to server
            if isempty(obj.stompClient)
                warning('StompClient not connected');
                return;
            end

            payload = struct(...
                'playerId', obj.getMyPlayer().playerID, ...
                'playerName', obj.getMyPlayer().name, ...
                'message', message);

            obj.stompClient.stompSend(sprintf('/app/room/%s/chat', obj.RoomId), payload);
        end

        function messages = getChatMessages(obj)
            % Get all chat messages
            messages = obj.chatMessages;
        end

        %% ===== GAME ACTION REQUESTS (Client → Server) =====

        function requestStartGame(obj)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/start', obj.RoomId), struct());
        end

        function requestRollDice(obj)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/roll', obj.RoomId), payload);
        end

        function requestBuyProperty(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'position', position);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/buyProperty', obj.RoomId), payload);
        end

        function requestDeclineProperty(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'position', position);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/declineProperty', obj.RoomId), payload);
        end

        function requestBuildHouse(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'position', position);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/buildHouse', obj.RoomId), payload);
        end

        function requestBuildHotel(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'position', position);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/buildHotel', obj.RoomId), payload);
        end

        function requestMortgage(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'position', position);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/mortgage', obj.RoomId), payload);
        end

        function requestUnmortgage(obj, position)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'position', position);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/unmortgage', obj.RoomId), payload);
        end

        function requestJailAction(obj, actionType)
            % actionType: 'PAY', 'CARD', or 'ROLL'
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'action', actionType);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/jailAction', obj.RoomId), payload);
        end

        function requestEndTurn(obj)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/endTurn', obj.RoomId), payload);
        end

        function requestPlaceBid(obj, amount)
            if isempty(obj.stompClient), warning('StompClient not connected'); return; end
            payload = struct('playerId', obj.getMyPlayer().playerID, 'amount', amount);
            obj.stompClient.stompSend(sprintf('/app/room/%s/game/placeBid', obj.RoomId), payload);
        end

        %% ===== HELPER METHODS =====

        function player = getCurrentPlayer(obj)
            % Get player whose turn it is
            player = obj.getPlayerById(obj.currentPlayerTurnId);
        end

        function player = getMyPlayer(obj)
            % Get local player (isClient = true)
            for i = 1:length(obj.Players)
                if obj.Players(i).isClient
                    player = obj.Players(i);
                    return;
                end
            end
            player = [];
        end

        function player = getPlayerById(obj, playerId)
            % Find player by ID
            for i = 1:length(obj.Players)
                if strcmp(obj.Players(i).playerID, playerId)
                    player = obj.Players(i);
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
                isTurn = strcmp(obj.currentPlayerTurnId, myPlayer.playerID);
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

            prop = obj.boardSpaces(myPlayer.position + 1);  % MATLAB 1-indexing
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
                prop = obj.boardSpaces(position + 1);  % MATLAB 1-indexing
            else
                prop = [];
            end
        end

        function players = getPlayersByPosition(obj, position)
            % Get all players at a specific position
            players = [];
            for i = 1:length(obj.Players)
                if obj.Players(i).position == position
                    players(end+1) = obj.Players(i);
                end
            end
        end

        function initializeBoard(obj, boardData)
            % Initialize board from server data
            obj.boardSpaces = [];
            for i = 1:length(boardData)
                spaceData = boardData(i);
                if strcmp(spaceData.spaceType, 'PROPERTY') || ...
                   strcmp(spaceData.spaceType, 'RAILROAD') || ...
                   strcmp(spaceData.spaceType, 'UTILITY')
                    obj.boardSpaces(i) = Property.fromServerData(spaceData);
                else
                    obj.boardSpaces(i) = BoardSpace.fromServerData(spaceData);
                end
            end
        end
    end
end
