package com.websockpoc.webapp;

import java.util.logging.Logger;
import javax.websocket.CloseReason;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket Server Endpoint implementation which currently appends "_server" to the received 
 * from the client message within the onMessage() method. Override to change this.
 * 
 * @author kloyevsk
 */
@ServerEndpoint( WebAppConstants.WEBSOCKET_SERVER_ENDPOINT_URL )
public class WebSocketServerEndpoint /* <T> */{

    //Attempted to pass Strategy Class handling onMessage() so that I wouldn't have to hack around
    //with the public static String lastServerMessageReceived;.
    //However, even if I do this, WebSocketServerEndpointTest wants a .class of WebSocketServerEndpoint
    //(ie. WebSocketServerEndpoint.class) and passing templatized t in there doesn't work !!!
    //      
    //      server = new Server( "localhost", 8080, "/websock-webapp", WebSocketServerEndpoint.class
    //private T t;
    //
    // public WebSocketServerEndpoint( T t ){
    //    this.t = t;
    //}
    
    private final static Logger logger = Logger.getLogger( WebSocketServerEndpoint.class.getName() );

    // HACK: needed for unit tests. Didn't find any way yet to obtain an instance of WebSocketServerEndpoint
    // from the Web Socket Server upon which I could store these as local variables and assert these during tests.
    public static String lastServerMessageReceived = null;    
    public static Session lastServerSession = null;
    
    @OnOpen
    public void onConnectionOpen(Session session) {
        logger.info( String.format( "<--- ServerEndpoint connection opened, sessionId=[%s]", session.getId() ) );
        
        lastServerSession = session;
    }

    @OnMessage
    public String onMessage(String message, Session session ) throws Exception {

        logger.info( String.format( "<--- ServerEndpoint received message=[%s], sessionId=[%s]", 
            message, ( session == null ? "" : session.getId() ) ) );
        
        lastServerMessageReceived = message;        
        
        if( message == null || message.isEmpty() ) return "Please send message";
        
        if( message.endsWith( WebAppConstants.CLIENT_PROCESSED_MESSAGE_SUFFIX ) ){
            // IMPORTANT: since there is a full-duplex connection, need some way to terminate the session
            // so as to avoid continously (but not indefinitely, since I don't see infinite loop) passing message 
            // between Client and Server. So simple implementation is to terminate session upon ServerEndpoint receiving 
            // message which was already processed by the ClientEndpoint (in response to ServerEndpoint-initiated initial message). 
            // This is only relevant to Java-based WebSocketClientEndpoint Client (not web page).
            session.close( new CloseReason( CloseReason.CloseCodes.NORMAL_CLOSURE, 
                "ClientEndpoint returned processed message, in response to original ServerEndpoint-initiated message." ) ); 
            return null;
        }
        else {
            // Append ServerEndpoint processed message suffix to the message received from ClientEndpoint to indicate 
            // to the ClientEndpoint that ServerEndpoint processed ClientEndpoint-initiated message (which in case of 
            // the Java-based WebSocketClientEndpoint is used to terminate the session; in case of a web page, it's just displayed).
            return message + WebAppConstants.SERVER_PROCESSED_MESSAGE_SUFFIX;
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.severe(String.format( "<--- ServerEndpoint encountered an error=[%s], sessionId=[%s]", 
            t.getMessage(), session.getId() ) );
    }
    
    @OnClose
    public void onConnectionClose(Session session, CloseReason closeReason) {
        logger.info( String.format( "<--- ServerEndpoint connection closed due to reason=[%s], sessionId=[%s]", 
            ( closeReason == null ? "" : closeReason.toString() ), ( session == null ? "" : session.getId() ) ) );
        
        lastServerSession = null;
    }
}
