classdef Card < handle
    % Card - Represents a Chance or Community Chest card

    properties
        cardType        % 'CHANCE' or 'COMMUNITY_CHEST'
        description     % Card text to display
        actionType      % Action to execute
        actionValue     % Value for action (amount, position, etc.)
    end

    methods
        function obj = Card(cardType, description, actionType, actionValue)
            if nargin > 0
                obj.cardType = cardType;
                obj.description = description;
                obj.actionType = actionType;
                if nargin >= 4
                    obj.actionValue = actionValue;
                end
            end
        end

        function str = toString(obj)
            str = sprintf('[%s] %s', obj.cardType, obj.description);
        end
    end

    methods (Static)
        function obj = fromServerData(data)
            % Create Card from server JSON data
            obj = Card(data.cardType, data.description, data.actionType, data.value);
        end
    end
end
