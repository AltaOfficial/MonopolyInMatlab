import Player.*; % la player class
 
ws = websocket("ws://echo.websocket.org");


disp("Player name: ")
disp(Player("John Johnson").PlayerName)