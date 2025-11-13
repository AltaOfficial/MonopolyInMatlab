function response = webRequest(url, data)
    import matlab.net.*;
    import matlab.net.http.*;
    %WEBREQUEST Summary of this function goes here
    %   Detailed explanation goes here

    uri = URI(url);

    % if data is passed in, then we know we need to send a POST request
    if(nargin > 1)
        requestHeaders = HeaderField("Content-Type", "application/json");
        requestMethod = RequestMethod.POST;
        body = MessageBody(data);
        requestMessage = RequestMessage(requestMethod, requestHeaders, body);
        response = send(requestMessage, uri);
    else
        response = send(RequestMessage, uri);
    end
    
end

