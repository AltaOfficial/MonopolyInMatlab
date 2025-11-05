import Player.*; % la player class
import SimpleClient.*; % the websocket client class

function main()
    % adding matlab-websocket jar to dynamic java class path
    javaaddpath("matlab-websocket-1.6.jar", "-end");
    
    % this errors out, must fix in the future
    client = SimpleClient("wss://echo.websocket.org");

    % open la main menu
    app = main_menu();clc

end

main();