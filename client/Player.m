classdef Player 
    properties
        PlayerName string
    end

    methods
        function playerObj = Player(playerName)
            playerObj.PlayerName = playerName;
        end
    end
end