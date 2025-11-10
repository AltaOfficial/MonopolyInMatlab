classdef StompClient < WebSocketClient
    % Stomp Client
    %   This class extends the WebSocketClient class so we don't need to manually handle
    %   STOMP message formatting each time, these methods automatically take care of it.

    methods(Static)
        function obj = getInstance(connectionUrl)
            persistent websocketConnection;
            connectionExist = ~isempty(websocketConnection) && websocketConnection.Status == 1;

            % If the input argument is an empty array ([]), it indicates that we need to
            % reset or clear the websocketConnection and exit the function early.
            % Continuing would cause the function to attempt connecting to an empty value.
            if(nargin > 0 && isempty(connectionUrl))
                if(connectionExist)
                    try
                       websocketConnection.stompDisconnect();
                       websocketConnection.close();
                    catch 
                        % websocket connection already closed
                    end
                end
                return;
            end

            % on the first call we require a websocket url to be provided
            % and check to see if the instance has already been created
            if(~connectionExist)
                if(nargin > 0)
                    websocketConnection = StompClient(connectionUrl);
                else
                    error("On the first call to get instance, you must provide a websocket server url to connect to!");
                end
            end

            % if it is already created we return it or return the one
            % that was created
            obj = websocketConnection;
        end
    end

    methods(Access = private) % only the get instance function can call it
        function obj = StompClient(varargin)

            obj@WebSocketClient(varargin{:});
            % Auto-connect to STOMP ws server
            obj.stompConnect();

            if(obj.Status == 1)
                disp("Client is connected");
            end
        end
    end

    methods
        function stompConnect(obj)
            % This function connects to the websocket using the formatting stomp likes
            % we will connect using stomp version 1.2, so we can have
            % access to the latest features
            connectFrame = sprintf('CONNECT\naccept-version:1.2\nhost:/\n\n%c', 0);
            obj.send(connectFrame);
        end

        function stompDisconnect(obj)
            % This function disconnects from the websocket server
            disconnectFrame = sprintf('DISCONNECT\nreceipt:1\n\n%c', 0);
            obj.send(disconnectFrame);
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
            joinRoomDestination = sprintf("/monopoly/room/%d/join", roomId);
            subscribeToRoomDestination = sprintf("/room/%d", roomId);
            
            obj.stompSubscribe(subscribeToRoomDestination);
            obj.stompSend(joinRoomDestination, playerStruct);
            
        end

        % leave room
    end

    % these functions need to be defined as stated in the superclass
    methods(Access = protected)
        function onOpen(obj, message)
            % This function simply displays the message received
            fprintf('%s\n', message);
        end
        
        function onTextMessage(obj, message)
            % This function parses STOMP messages and routes to Game singleton
            fprintf('Message received:\n%s\n', message);

            try
                % Parse STOMP frame to extract JSON body
                lines = splitlines(message);

                % Find the frame type (first line)
                if ~isempty(lines) && ~isempty(lines{1})
                    frameType = strtrim(lines{1});

                    % Only process MESSAGE frames (not CONNECTED, RECEIPT, etc.)
                    if strcmp(frameType, 'MESSAGE')
                        % Find the empty line that separates headers from body
                        bodyStartIdx = 0;
                        for i = 1:length(lines)
                            if isempty(strtrim(lines{i}))
                                bodyStartIdx = i + 1;
                                break;
                            end
                        end

                        % Extract JSON body (everything after the empty line)
                        if bodyStartIdx > 0 && bodyStartIdx <= length(lines)
                            jsonBody = strjoin(lines(bodyStartIdx:end), '');

                            % Remove null terminator if present
                            jsonBody = strrep(jsonBody, char(0), '');
                            jsonBody = strtrim(jsonBody);

                            % Parse JSON and route to Game
                            if ~isempty(jsonBody)
                                msgJson = jsondecode(jsonBody);
                                game = Game.gameInstance();
                                game.processWebsocketMessage(msgJson);
                            end
                        end
                    end
                end
            catch ME
                fprintf('Error processing message: %s\n', ME.message);
            end
        end
        
        function onBinaryMessage(obj, bytearray)
            % This function simply displays the message received
            fprintf('Binary message received:\n');
            fprintf('Array length: %d\n', length(bytearray));
        end
        
        function onError(obj, message)
            % This function simply displays the message received
            fprintf('Error: %s\n', message);
        end
        
        function onClose(obj, message)
            % This function simply displays the message received
            fprintf('%s\n', message);
        end
       
    end
end

