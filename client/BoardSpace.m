classdef BoardSpace < handle
    % BoardSpace - Base class for non-property board spaces (GO, Jail, etc.)

    properties
        position        % Board position (0-39)
        name           % Space name
        spaceType           % 'CORNER', 'TAX', 'CHANCE', 'COMMUNITY_CHEST'
        taxAmount      % Tax amount (for tax spaces)
    end

    methods
        function obj = BoardSpace(position, name, spaceType, taxAmount)
            if nargin > 0
                obj.position = position;
                obj.name = name;
                obj.spaceType = spaceType;
                if nargin >= 4
                    obj.taxAmount = taxAmount;
                else
                    obj.taxAmount = 0;
                end
            end
        end

        function str = toString(obj)
            str = sprintf('%s (Position %d)', obj.name, obj.position);
        end
    end

    methods (Static)
        function obj = fromServerData(data)
            % Create BoardSpace from server JSON data
            obj = BoardSpace(data.position, data.name, data.spaceType, data.taxAmount);
        end
    end
end
