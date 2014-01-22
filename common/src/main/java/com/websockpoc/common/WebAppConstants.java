package com.websockpoc.common;

/**
 * Defines common constants used everywhere else.
 * 
 * @author kloyevsk
 */
public class WebAppConstants {

    // Note that currently /websock-poc/webapp/src/main/webapp/index.html hardcodes full
    // ws URI as ws://127.0.0.1:8080/websock-webapp/echo. Need to find a way to instead
    // use below constants.

    // Could be wss:// for Secure WebSocket
    public final static String WEBSOCKET_PROTOCOL = "ws://";
    public final static String WEBSOCKET_HOST = "127.0.0.1";
    public final static int WEBSOCKET_PORT = 8080;
    
    // Web app name without .war
    public final static String WEBSOCKET_WEBAPP_NAME = "websock-webapp";
    // Note : this should match the name of the /webapp .war (without .war part) specified in 
    // the /webapp/pom.xml in maven-war-plugin section with <warName>websock-webapp</warName>
    public final static String WEBSOCKET_WEBAPP_URL = "//" + WEBSOCKET_WEBAPP_NAME;
    public final static String WEBSOCKET_SERVER_ENDPOINT_URL = "/echo";
    
    // "ws://127.0.0.1:8080/websock-webapp/echo
    public final static String WEBSOCKET_FULL_URL = 
        WEBSOCKET_PROTOCOL + WEBSOCKET_HOST + ":" + WEBSOCKET_PORT + WEBSOCKET_WEBAPP_URL + WEBSOCKET_SERVER_ENDPOINT_URL;
    
    // Suffixes added to server and client processed messages (indicates to the other side that
    // message was received and processed, so that session could be terminated (rather than sending
    // messages back and force between server and client). See WebSocketServerEndpoint and
    // WebSocketClientEndpoint classes for more on this.
    public final static String SERVER_PROCESSED_MESSAGE_SUFFIX = "_server";
    public final static String CLIENT_PROCESSED_MESSAGE_SUFFIX = "_client";
}
