package com.websockpoc.common;

import java.util.logging.Logger;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 * WebSocket Client Endpoint which currently appends "_client" to the received 
 * from the server message within the onMessage() method. Override to change this.
 * Currently used in unit / integrations tests only.
 * 
 * @author kloyevsk
 */
@ClientEndpoint
public class WebSocketClientEndpoint {

    private static final Logger logger = Logger.getLogger( WebSocketClientEndpoint.class.getName() );

    // HACK: needed for unit tests. Didn't find any way yet to obtain an instance of WebSocketServerEndpoint
    // from the Web Socket Server upon which I could store these as local variables and assert these during tests.
    public static String lastClientMessageReceived;
    
    @OnOpen
    public void onOpen(Session session) {
        logger.info( String.format( "---> ClientEndpoint connected, sessionId=[%s]", session.getId() ) );        
    }
 
    @OnMessage
    public String onMessage(String message, Session session) throws Exception {
        
        logger.info( String.format( "---> ClientEndpoint received message=[%s], sessionId=[%s]", message, session.getId() ) );
        
        lastClientMessageReceived = message;
        
        if( message == null || message.isEmpty() ) return "Please send message";
        
        if( message.endsWith( WebAppConstants.SERVER_PROCESSED_MESSAGE_SUFFIX ) ){
            // IMPORTANT: since there is a full-duplex connection, need some way to terminate the session
            // so as to avoid continously (but not indefinitely, since I don't see infinite loop) passing message 
            // between Client and Server. So simple implementation is to terminate session upon ClientEndpoint receiving 
            // message which was already processed by the ServerEndpoint (in response to ClientEndpoint-initiated initial message). 
            // This is only relevant to Java-based WebSocketClientEndpoint Client (not web page).
            session.close( new CloseReason( CloseReason.CloseCodes.NORMAL_CLOSURE, 
                "ServerEndpoint returned processed message, in response to original ClientEndpoint-initiated message." ) ); 
            return null;
        }
        else {
            // Append ClientEndpoint processed message suffix to the message received from ServerEndpoint to indicate 
            // to the ServerEndpoint that ClientEndpoint processed ServerEndpoint-initiated message (which in case of 
            // the Java-based WebSocketServerEndpoint is used to terminate the session).
            return message + WebAppConstants.CLIENT_PROCESSED_MESSAGE_SUFFIX;
        }        
    }
 
    @OnError
    public void onError(Session session, Throwable t) {
        logger.severe(String.format( "---> ClientEndpoint encountered an error=[%s], sessionId=[%s]", 
            ( t == null ? "" : t.getMessage() ), ( session == null ? "" : session.getId() ) ) );
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info( String.format( "---> ClientEndpoint disconnected due to reason=[%s], sessionid=[%s]", 
            ( closeReason == null ? "" : closeReason.toString() ), ( session == null ? "" : session.getId() ) ) );
    }
}
