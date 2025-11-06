import Player.*; % la player class
import StompClient.*; % the websocket client class

function main()
    % adding matlab-websocket jar to dynamic java class path
    javaaddpath("matlab-websocket-1.6.jar", "-end");
    
    % this gives a java bean error, but it still connects to the websocket
    client = StompClient("ws://localhost:8000/ws");

    client.joinRoom(123, "barbelly dillon");

    pause(5);


    % open la main menu
    app = main_menu();

end

main();

% helpful links
% https://www.youtube.com/watch?v=TywlS9iAZCM - Spring boot & Websockets