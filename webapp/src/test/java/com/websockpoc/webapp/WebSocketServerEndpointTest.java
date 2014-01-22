package com.websockpoc.webapp;

import com.websockpoc.common.WebSocketClientEndpoint;
import com.websockpoc.common.WebAppConstants;
import java.net.URI;
import javax.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * WebSocket Server Endpoint test using Tyrus (Glassfish project's WebSocket's implementation)
 * Web Socket Server.
 * 
 * @author kloyevsk
 */

// KL - I am very UNHAPPY that I needed to pollute actual EndPoint classes (WebSocketServerEndpoint
// and WebSocketClientEndpoint) with extra public static variables just for the sake of unit testing them
// due to various APIs requiring .class vs. an object of the .class.
// Note that can pass instances of WebSocketClientEndpoint class with 
//      Session clientSession = client.connectToServer( new WebSocketClientEndpoint(),
//          new URI("ws://localhost:8080/websock-webapp/echo") );
// rather than passing its class, as currently done with
//      Session clientSession = client.connectToServer( WebSocketClientEndpoint.class,
//            new URI("ws://localhost:8080/websock-webapp/echo") );
// However, can't pass instances of WebSocketServerEndpoint .class, since signatures of 
// org.glassfish.tyrus.server.Server all require usage of the WebSocketServerEndpoint.class vs.
// its objects. 
// 
// TODO:
// 1.   Need to go back and look at whether making EndPoints programmatic (rather than Annotations-based as 
// they are now) will be a better implementation that won't do that. 
// 2.   Look at https://tyrus.java.net/apidocs/1.0/index.html?org/glassfish/tyrus/server/Server.html javadocs
// to see whether can use other means of instantiating the Server without requiring .class of the ServerEndpoint
// (like with javax.websocket.server.ServerApplicationConfig or javax.websocket.server.ServerEndpointConfig)
//
public class WebSocketServerEndpointTest {

    private static Server server = null;

    @BeforeClass
    public static void createAndStartServer() throws Exception {  
        // KL - note that 3rd parameter is the rootPath - meaning the root of the web application context
        // (ex. /websock-webapp). The complete WebSocket's ws uri is obtained by combining the
        // rootPath + value of @ServerEndpoint annotation in WebSocketServerEndpoint (ie. "/echo")
        // to yield /websock-webapp/echo (so the complete uri is ws://localhost:8080/websock-webapp/echo).        
        server = new Server( WebAppConstants.WEBSOCKET_HOST, WebAppConstants.WEBSOCKET_PORT, WebAppConstants.WEBSOCKET_WEBAPP_URL, 
            WebSocketServerEndpoint.class );
               //((Class)  ((ParameterizedType) WebSocketServerEndpoint.class.getGenericSuperclass()).getActualTypeArguments()[0] )
                /* Class.forName( "com.websockpoc.bapp.WebSocketServerEndpoint" */
        server.start();
    }
 
    @AfterClass
    public static void stopServer(){    
        if( server != null ) server.stop();
    }
    
    @Before
    public void before() {
    }
    
    @After
    public void after(){
        // Nullify static variables after each test, in preparation for next test
        WebSocketClientEndpoint.lastClientMessageReceived = null;
        WebSocketServerEndpoint.lastServerMessageReceived = null;
        // IMPORTANT: a new ServerEndpoint Session is created right after a new ClientEndpoint
        // is created, with below code
        //      ClientManager client = ClientManager.createClient();
        //      client.connectToServer( WebSocketClientEndpoint.class,
        //            new URI("ws://localhost:8080/websock-webapp/echo") );
        // Since each test method creates a new Client Endpoint, it's safe to nullify
        // WebSocketServerEndpoint.lastServerSession after each method finishes in here.
        WebSocketServerEndpoint.lastServerSession = null;
    }
      
    @Test
    public void testWebSocketMessageInitiatedByClientEndPoint() throws Exception {
        
        System.out.println( "-- testWebSocketMessageInitiatedByClientEndPoint START --" );
        
        assertTrue( WebSocketClientEndpoint.lastClientMessageReceived == null );
        assertTrue( WebSocketServerEndpoint.lastServerMessageReceived == null );
        assertTrue( WebSocketServerEndpoint.lastServerSession == null );
        
        ClientManager client = ClientManager.createClient();
        // Note: could also pass an instance of the WebSocketClientEndpoint.class with
        //      Session clientSession = client.connectToServer( new WebSocketClientEndpoint(),
        // rather than passing its class.
        Session clientSession = client.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );

        //
        // After clientSession is initiated, ClientEndpoint sends message to ServerEndpoint
        //
        String clientInitiatedMessage = "clientInitiatedMessage_1";
        clientSession.getBasicRemote().sendText( clientInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ServerEndpoint received clientInitiatedMessage as-is
        assertTrue( clientInitiatedMessage.equals( WebSocketServerEndpoint.lastServerMessageReceived ) );
        // Assert that ClientEndpoint received ( clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX)
        // back from the ServerEndpoint
        assertTrue( ( clientInitiatedMessage + WebAppConstants.SERVER_PROCESSED_MESSAGE_SUFFIX).equals( 
            WebSocketClientEndpoint.lastClientMessageReceived ) );

        // Since server sent back (clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX) 
        // to client, client terminated the session.
        assertFalse( clientSession.isOpen() );
        
        // 
        // Since client session was terminated, need to start a new one before ClientEndpoint sending
        // message to ServerEndpoint second time
        //
        clientSession = client.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        
        clientInitiatedMessage = "clientInitiatedMessage_2";
        clientSession.getBasicRemote().sendText( clientInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ServerEndpoint received clientInitiatedMessage as-is
        assertTrue( clientInitiatedMessage.equals( WebSocketServerEndpoint.lastServerMessageReceived ) );
        // Assert that ClientEndpoint received ( clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX)
        // back from the ServerEndpoint
        assertTrue( ( clientInitiatedMessage + WebAppConstants.SERVER_PROCESSED_MESSAGE_SUFFIX).equals( 
            WebSocketClientEndpoint.lastClientMessageReceived ) );
        
        // Since server sent back (clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX) 
        // to client, client terminated the session.
        assertFalse( clientSession.isOpen() );
        
        System.out.println( "-- testWebSocketMessageInitiatedByClientEndPoint END --" );
    }
    
    @Test
    public void testWebSocketMessageInitiatedByServerEndPoint() throws Exception {
        
        System.out.println( "-- testWebSocketMessageInitiatedByServerEndPoint START --" );
        
        assertTrue( WebSocketClientEndpoint.lastClientMessageReceived == null );
        assertTrue( WebSocketServerEndpoint.lastServerMessageReceived == null );
        assertTrue( WebSocketServerEndpoint.lastServerSession == null );
        
        ClientManager client = ClientManager.createClient();
        // Create ClientEndpoint
        client.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        
        assertTrue( WebSocketServerEndpoint.lastServerSession != null );
        
        //
        // After serverSession is initiated, ServerEndpoint sends message to ClientEndpoint ...
        //
        String serverInitiatedMessage = "serverInitiatedMessage_1";
        WebSocketServerEndpoint.lastServerSession.getBasicRemote().sendText( serverInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ClientEndpoint received serverInitiatedMessage as-is
        assertTrue( serverInitiatedMessage.equals( WebSocketClientEndpoint.lastClientMessageReceived ) );
        // Assert that ServerEndpoint received ( serverInitiatedMessage + WebSocketClientEndpoint.CLIENT_PROCESSED_MESSAGE_SUFFIX)
        // back from the ClientEndpoint
        assertTrue( ( serverInitiatedMessage + WebAppConstants.CLIENT_PROCESSED_MESSAGE_SUFFIX).equals( 
            WebSocketServerEndpoint.lastServerMessageReceived ) );

        // Since client sent back (clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX) 
        // to server, server terminated the session (calling its 
        //      public void onConnectionClose(Session session, CloseReason closeReason) {
        // null-ing out the static lastServerSession
        assertNull( WebSocketServerEndpoint.lastServerSession );

        //
        // Since server session was terminated, need to start a new one before ServerEndpoint sends message 
        // to ClientEndpoint second time. Can create a new Server this by creating a new ***Client Session*** !!!
        //
        client.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        assertTrue( WebSocketServerEndpoint.lastServerSession != null );
        serverInitiatedMessage = "serverInitiatedMessage_2";
        WebSocketServerEndpoint.lastServerSession.getBasicRemote().sendText( serverInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ClientEndpoint received serverInitiatedMessage as-is
        assertTrue( serverInitiatedMessage.equals( WebSocketClientEndpoint.lastClientMessageReceived ) );
        // Assert that ServerEndpoint received ( serverInitiatedMessage + WebSocketClientEndpoint.CLIENT_PROCESSED_MESSAGE_SUFFIX)
        // back from the ClientEndpoint
        assertTrue( ( serverInitiatedMessage + WebAppConstants.CLIENT_PROCESSED_MESSAGE_SUFFIX).equals( 
            WebSocketServerEndpoint.lastServerMessageReceived ) );
        
        // Since client sent back (clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX) 
        // to server, server terminated the session (calling its 
        //      public void onConnectionClose(Session session, CloseReason closeReason) {
        // null-ing out the static lastServerSession
        assertNull( WebSocketServerEndpoint.lastServerSession );
                
        System.out.println( "-- testWebSocketMessageInitiatedByServerEndPoint END --" );
    }
}
