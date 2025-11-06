classdef StompClient < SimpleClient
    %CLIENT Summary of this class goes here
    %   Detailed explanation goes here
    
    properties
    end
    
    methods
        function obj = StompClient(varargin)
            %Constructor
            % okay so for my reference @ means to call the constructor of
            % something

            % varargin{:}, the {:} is like a spread operator
            % and I thought rust syntax was strange

            % so obj is passed into the constructor
            obj@SimpleClient(varargin{:});
            % Auto-connect to STOMP ws server
            obj.stompConnect();
            clc
            % giving client time to connect to the ws server
            pause(2);

            if(obj.Status == 1)
                disp("Client is connected");
            end
        end
    
        function stompConnect(obj)
            % This function connects to the websocket using the formatting stomp likes
            % we will connect using stomp version 1.2, so we can have
            % access to the latest features
            connectFrame = sprintf('CONNECT\naccept-version:1.2\nhost:/\n\n%c', 0);
            obj.send(connectFrame);
        end

        function stompSubscribe(obj, destination, subscriptionId)
            if(nargin < 3)
                subscriptionId = "room-sub";
            end

            subscribeFrame = sprintf(['SUBSCRIBE\n'...
                'id:%s\n'...
                'destination:%s\n'...
                '\n%c'], ...
                subscriptionId, destination, 0);
            obj.send(subscribeFrame);
        end

        function stompSend(obj, destination, body)
            if(isstruct(body) || isobject(body))
                body = jsonencode(body);
            end

            sendFrame = sprintf(['SEND\n'...
                'destination:%s\n'...
                'content-type:application/json\n'...
                'content-length:%d\n\n'...
                '%s%c'], ...
                destination, length(body), body, 0);
            
            obj.send(sendFrame);
        end

        function joinRoom(obj, roomId, playerName)
            playerStruct = struct("playerName", playerName);
            destination = sprintf("/monopoly/room/%d/join", roomId);
            
            obj.stompSubscribe(destination);
            obj.stompSend(destination, playerStruct);
            
        end
       
    end
end

