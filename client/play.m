import Player.*; % la player class
import StompClient.*; % the websocket client class
import SaveWebsocketsLibrary.*; % function to install websocket library

clc;
SaveWebsocketsLibrary();

% Clear any previous websocket connections
StompClient.getInstance([]);

% Connect to the websocket server
StompClient.getInstance("ws://localhost:8000/ws");

% open la main menu
main_menu();

% HELPFUL LINKS

% Spring boot & Websockets
% https://www.youtube.com/watch?v=TywlS9iAZCM