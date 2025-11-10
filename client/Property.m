classdef Property < handle
    % Property - Represents a purchasable property, railroad, or utility

    properties
        position            % Board position (1-40, using 1-indexing for MATLAB)
        name               % Property name
        type               % 'PROPERTY', 'RAILROAD', 'UTILITY'
        colorGroup         % Color group string
        owner              % Player object who owns this

        % Pricing
        purchasePrice      % Purchase price
        mortgageValue      % Mortgage value (half of purchase)
        isMortgaged        % Mortgage status

        % Rent structure (for properties)
        rentBase           % Base rent
        rent1House         % Rent with 1 house
        rent2House         % Rent with 2 houses
        rent3House         % Rent with 3 houses
        rent4House         % Rent with 4 houses
        rentHotel          % Rent with hotel

        % Building costs
        houseCost          % Cost to build one house
        hotelCost          % Cost to build hotel
        housesBuilt        % Number of houses (0-4)
        hasHotel           % Boolean - has hotel
    end

    methods
        function obj = Property(position, name, type)
            if nargin > 0
                obj.position = position;
                obj.name = name;
                obj.type = type;
                obj.isMortgaged = false;
                obj.housesBuilt = 0;
                obj.hasHotel = false;
            end
        end

        function rent = calculateCurrentRent(obj, diceRoll, ownerRRCount, ownerUtilityCount, hasMonopoly)
            % Calculate rent based on property type and state
            if obj.isMortgaged
                rent = 0;
                return;
            end

            switch obj.type
                case 'PROPERTY'
                    if obj.hasHotel
                        rent = obj.rentHotel;
                    elseif obj.housesBuilt > 0
                        switch obj.housesBuilt
                            case 1, rent = obj.rent1House;
                            case 2, rent = obj.rent2House;
                            case 3, rent = obj.rent3House;
                            case 4, rent = obj.rent4House;
                            otherwise, rent = obj.rentBase;
                        end
                    else
                        % No houses - check for monopoly double rent
                        if hasMonopoly
                            rent = obj.rentBase * 2;
                        else
                            rent = obj.rentBase;
                        end
                    end

                case 'RAILROAD'
                    switch ownerRRCount
                        case 1, rent = 25;
                        case 2, rent = 50;
                        case 3, rent = 100;
                        case 4, rent = 200;
                        otherwise, rent = 0;
                    end

                case 'UTILITY'
                    if ownerUtilityCount == 2
                        rent = diceRoll * 10;
                    else
                        rent = diceRoll * 4;
                    end

                otherwise
                    rent = 0;
            end
        end

        function can = canBuildHouse(obj)
            can = strcmp(obj.type, 'PROPERTY') && ~obj.isMortgaged && obj.housesBuilt < 4 && ~obj.hasHotel;
        end

        function can = canBuildHotel(obj)
            can = strcmp(obj.type, 'PROPERTY') && ~obj.isMortgaged && obj.housesBuilt == 4 && ~obj.hasHotel;
        end

        function can = canMortgage(obj)
            can = ~obj.isMortgaged && obj.housesBuilt == 0 && ~obj.hasHotel;
        end

        function str = toString(obj)
            if ~isempty(obj.owner)
                ownerName = obj.owner.name;
            else
                ownerName = 'Unowned';
            end
            str = sprintf('%s [%s] - Owner: %s', obj.name, obj.type, ownerName);
        end
    end

    methods (Static)
        function obj = fromServerData(data)
            % Create Property from server JSON data
            obj = Property(data.position, data.name, data.spaceType);

            % Set color group
            if isfield(data, 'colorGroup')
                obj.colorGroup = data.colorGroup;
            end

            % Set pricing
            if isfield(data, 'purchasePrice')
                obj.purchasePrice = data.purchasePrice;
            end
            if isfield(data, 'mortgageValue')
                obj.mortgageValue = data.mortgageValue;
            end

            % Set rent structure
            if isfield(data, 'rentBase')
                obj.rentBase = data.rentBase;
                obj.rent1House = data.rent1House;
                obj.rent2House = data.rent2House;
                obj.rent3House = data.rent3House;
                obj.rent4House = data.rent4House;
                obj.rentHotel = data.rentHotel;
            end

            % Set building costs
            if isfield(data, 'houseCost')
                obj.houseCost = data.houseCost;
                obj.hotelCost = data.hotelCost;
            end

            % Set state
            if isfield(data, 'isMortgaged')
                obj.isMortgaged = data.isMortgaged;
            end
            if isfield(data, 'housesBuilt')
                obj.housesBuilt = data.housesBuilt;
            end
            if isfield(data, 'hasHotel')
                obj.hasHotel = data.hasHotel;
            end
        end
    end
end