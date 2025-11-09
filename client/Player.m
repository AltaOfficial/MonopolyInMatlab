classdef Player
    %GAME Summary of this class goes here
    %   Detailed explanation goes here

    properties
        playerID
        name
        position = 1 % Start at GO
        money = 1500 % Starting money
        ownedProperties % Array of Property objects
        inJail = false
        jailTurns = 0
        getOutOfJailCards = 0
        isBankrupt = false
        isClient = true % since there will me multiple players in a room, we need to signify which one is us
    end

    methods
        function obj = Player() % only the get instance function can call it
            
        end
    end

    methods
        function processWebsocketMessage()
            
        end
    end
end