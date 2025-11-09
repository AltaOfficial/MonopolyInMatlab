classdef Game
    %GAME Summary of this class goes here
    %   Detailed explanation goes here

    properties
        RoomId
        Players % all of the players in the room
        messages
        boardProperties = []
        weAreHost = false
        currentPlayerTurnId % The id of the player whos turn it currently is
    end

    methods(Static)
        function obj = gameInstance()
            persistent game;

            if(isempty(game))
                game = Game();
            end

            obj = game;
        end
    end

    methods(Access = private)
        function obj = Game(player, roomId) % only the get instance function can call it
            
        end

    end

    methods
        function processWebsocketMessage()
            
        end
    end
end