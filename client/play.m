% get me out of the this dogwater ide
import Player.*; % la player class
import StompClient.*; % the websocket client class
import SaveWebsocketsLibrary.*; % function to install websocket library

clc;
clear;
SaveWebsocketsLibrary();
dev_mode = false; % change this if not developing

% Clear any previous websocket connections
StompClient.getInstance([]);

% Connect to the websocket server
if(dev_mode == true)
    StompClient.getInstance("ws://localhost:8000/ws");
else
    StompClient.getInstance("wss://monopolyinmatlabserver-production.up.railway.app/ws");
end

main_menu(dev_mode);

% HELPFUL LINKS

% Spring boot & Websockets
% https://www.youtube.com/watch?v=TywlS9iAZCM