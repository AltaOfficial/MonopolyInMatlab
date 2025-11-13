classdef Player < handle
    % Player - Represents a player in the Monopoly game (matches server GamePlayer)

    properties
        % Player fields (from server GamePlayer)
        playerId           % UUID string
        playerName         % Player name

        % Game fields
        position = 0       % Board position (0-39, 0-indexed to match server)
        money = 1500       % Current money
        ownedPropertyPositions  % Array of integers (property positions)
        inJail = false     % Jail status
        jailTurns = 0      % Turns spent in jail
        getOutOfJailCards = 0   % Get Out of Jail Free cards
        isBankrupt = false      % Bankruptcy status
        colorGroupCounts   % containers.Map of color group counts (ColorGroup -> count)
        totalHouses = 0    % Total houses owned
        totalHotels = 0    % Total hotels owned

        % Client-only properties
        isClient = false   % True if this is the local player
        ownedProperties    % Array of Property objects (client-side only for UI)
        monopolies         % Cell array of color groups where player has monopoly
        netWorth = 0       % Calculated net worth
    end

    methods
        function obj = Player(playerId, playerName, isClient)
            % Constructor
            if nargin >= 2
                obj.playerId = playerId;
                obj.playerName = playerName;
                if nargin >= 3
                    obj.isClient = isClient;
                end
            end
            obj.ownedPropertyPositions = [];
            obj.ownedProperties = [];
            obj.colorGroupCounts = containers.Map();
            obj.monopolies = {};
        end

        function updateFromServerData(obj, playerData)
            % Sync player state from server broadcast
            if isfield(playerData, 'playerId')
                obj.playerId = playerData.playerId;
            end
            if isfield(playerData, 'playerName')
                obj.playerName = playerData.playerName;
            end
            if isfield(playerData, 'position')
                obj.position = playerData.position;
            end
            if isfield(playerData, 'money')
                obj.money = playerData.money;
            end
            if isfield(playerData, 'ownedPropertyPositions')
                obj.ownedPropertyPositions = playerData.ownedPropertyPositions;
            end
            if isfield(playerData, 'inJail')
                obj.inJail = playerData.inJail;
            end
            if isfield(playerData, 'jailTurns')
                obj.jailTurns = playerData.jailTurns;
            end
            if isfield(playerData, 'getOutOfJailCards')
                obj.getOutOfJailCards = playerData.getOutOfJailCards;
            end
            if isfield(playerData, 'isBankrupt')
                obj.isBankrupt = playerData.isBankrupt;
            end
            if isfield(playerData, 'colorGroupCounts')
                % Convert server map to MATLAB containers.Map
                obj.colorGroupCounts = containers.Map();
                fields = fieldnames(playerData.colorGroupCounts);
                for i = 1:length(fields)
                    obj.colorGroupCounts(fields{i}) = playerData.colorGroupCounts.(fields{i});
                end
            end
            if isfield(playerData, 'totalHouses')
                obj.totalHouses = playerData.totalHouses;
            end
            if isfield(playerData, 'totalHotels')
                obj.totalHotels = playerData.totalHotels;
            end
        end

        function addProperty(obj, propertyObj)
            % Add property to owned properties (client-side)
            obj.ownedProperties(end+1) = propertyObj;
            propertyObj.owner = obj;

            % Add position to ownedPropertyPositions (matches server)
            if ~ismember(propertyObj.position, obj.ownedPropertyPositions)
                obj.ownedPropertyPositions(end+1) = propertyObj.position;
            end

            % Update color group count
            if ~isempty(propertyObj.colorGroup)
                if obj.colorGroupCounts.isKey(propertyObj.colorGroup)
                    obj.colorGroupCounts(propertyObj.colorGroup) = ...
                        obj.colorGroupCounts(propertyObj.colorGroup) + 1;
                else
                    obj.colorGroupCounts(propertyObj.colorGroup) = 1;
                end
            end

            % Update monopolies
            obj.updateMonopolies();
        end

        function removeProperty(obj, propertyObj)
            % Remove property from owned properties
            obj.ownedProperties(obj.ownedProperties == propertyObj) = [];
            propertyObj.owner = [];

            % Remove position from ownedPropertyPositions
            obj.ownedPropertyPositions(obj.ownedPropertyPositions == propertyObj.position) = [];

            % Update color group count
            if ~isempty(propertyObj.colorGroup) && obj.colorGroupCounts.isKey(propertyObj.colorGroup)
                count = obj.colorGroupCounts(propertyObj.colorGroup);
                if count > 1
                    obj.colorGroupCounts(propertyObj.colorGroup) = count - 1;
                else
                    remove(obj.colorGroupCounts, propertyObj.colorGroup);
                end
            end

            % Update monopolies
            obj.updateMonopolies();
        end

        function updateMonopolies(obj)
            % Update list of monopolies based on color group counts
            obj.monopolies = {};
            keys = obj.colorGroupCounts.keys();
            for i = 1:length(keys)
                colorGroup = keys{i};
                count = obj.colorGroupCounts(colorGroup);
                required = obj.getRequiredForMonopoly(colorGroup);
                if count >= required
                    obj.monopolies{end+1} = colorGroup;
                end
            end
        end

        function required = getRequiredForMonopoly(obj, colorGroup)
            % Get number of properties required for monopoly
            switch colorGroup
                case {'BROWN', 'DARK_BLUE'}
                    required = 2;
                case 'RAILROAD'
                    required = 4;
                case 'UTILITY'
                    required = 2;
                otherwise
                    required = 3;  % Most color groups
            end
        end

        function has = ownsMonopoly(obj, colorGroup)
            % Check if player owns monopoly in color group
            has = ismember(colorGroup, obj.monopolies);
        end

        function can = canAfford(obj, amount)
            % Check if player can afford amount
            can = obj.money >= amount;
        end

        function props = getPropertiesOfColor(obj, colorGroup)
            % Get all properties of a specific color group
            props = obj.ownedProperties(arrayfun(@(p) strcmp(p.colorGroup, colorGroup), obj.ownedProperties));
        end

        function worth = calculateNetWorth(obj)
            % Calculate total net worth (money + property values)
            worth = obj.money;
            for i = 1:length(obj.ownedProperties)
                prop = obj.ownedProperties(i);
                if ~prop.isMortgaged
                    worth = worth + prop.purchasePrice;
                end
                worth = worth + prop.housesBuilt * prop.houseCost;
                if prop.hasHotel
                    worth = worth + prop.hotelCost;
                end
            end
            obj.netWorth = worth;
        end

        function moveToPosition(obj, newPos)
            % Update position
            obj.position = newPos;
        end

        function sendToJail(obj)
            % Send player to jail
            obj.position = 10;  % Jail position
            obj.inJail = true;
            obj.jailTurns = 0;
        end

        function releaseFromJail(obj)
            % Release player from jail
            obj.inJail = false;
            obj.jailTurns = 0;
        end

        function str = toString(obj)
            % String representation
            str = sprintf('%s: $%d at position %d', obj.playerName, obj.money, obj.position);
        end
    end

    methods (Static)
        function obj = fromServerData(data)
            % Create Player from server JSON data (GamePlayer)
            obj = Player(data.playerId, data.playerName, false);
            obj.updateFromServerData(data);
        end
    end
end
