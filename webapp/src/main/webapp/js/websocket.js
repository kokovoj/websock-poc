// TODO : need to make web-app name (ie. websock-webapp and WebSocket url (ie. websock-webapp/echo)
// configurable, rather than hardcoded. On the Java side all constants are contained within 
// /websock-poc/webapp/src/main/java/com/websockpoc/webapp/WebAppConstants.java class. Need to 
// figure out how to propagate them here as well.
var ws = $.gracefulWebSocket( "ws://127.0.0.1:8080/websock-webapp/echo" );
ws.onmessage = function(event) {
    var messageFromServer = event.data;
    $('#output').append('<p>Received from server: '+messageFromServer+'</p>');
}

function send(message) {
    ws.send(message);
}