package com.websockpoc.wildfly;

import com.websockpoc.webapp.WebAppConstants;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.After;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;



/**
 * WebSocket Server Endpoint test using handlers in the new Undertow Web Server 
 * (which is one used by Wildfly 8 AS). WebSocketClient used is Jetty's.
 * 
 * This is an alternative way (to deploying a WebSockets Web Application in .war) 
 * of implementing Web Sockets using Undertow, which is pretty cool.
 * 
 * @author kloyevsk
 */
public class WebSocketUndertowHandlersTest {

    // An embedded instance of the Undertow Web Server which we use during unit tests
    private Undertow server;
    private String lastReceivedMessage;
    
    private PathHandler getWebSocketHandler() {
        return path()
            // "/websock-webapp/echo"
            .addPath( WebAppConstants.WEBSOCKET_WEBAPP_URL + WebAppConstants.WEBSOCKET_SERVER_ENDPOINT_URL,  
                websocket(new WebSocketConnectionCallback() {

                    @Override
                    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                        channel.getReceiveSetter().set(new AbstractReceiveListener() {
                            @Override
                            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                String data = message.getData();
                                lastReceivedMessage = data;
                                // logger.info("Received data: " + data);
                                System.out.println( "Server received data: " + data );
                                WebSockets.sendText(data, channel, null);
                            }
                        });
                        channel.resumeReceives();
                    }
                })
            )
            .addPath( "/", 
                resource(new ClassPathResourceManager(
                            WebSocketUndertowHandlersTest.class.getClassLoader(), 
                            WebSocketUndertowHandlersTest.class.getPackage())
                        )
                        .addWelcomeFiles("index.html")
            );
    }
    
    @Before
    public void before() {
        server = Undertow.builder()
                .addListener( WebAppConstants.WEBSOCKET_PORT, WebAppConstants.WEBSOCKET_HOST )
                .setHandler(getWebSocketHandler())
                .build();
        server.start();
    }
    
    @After
    public void after(){
        if (server != null) {
            server.stop();
        }
        
        lastReceivedMessage = null;
    }
    
    @Test
    public void testWebSocketOne() throws Exception {
        startWebSocketClientAndAssertLastReceivedMessage( "TestMessage" );
    }
    
    @Test
    public void testWebSocketTwo() throws Exception {
        startWebSocketClientAndAssertLastReceivedMessage( "AnotherTestMessage" );
    }

    @Test
    public void testWebSocketThree() throws Exception {
        startWebSocketClientAndAssertLastReceivedMessage( ( new Date() ).toString() );
    }
    
    private void startWebSocketClientAndAssertLastReceivedMessage( final String theLastReceivedMessage ) throws Exception {
        
        WebSocketClient client = new WebSocketClient();
        Future<WebSocket.Connection> connectionFuture = 
            // "ws://localhost:8080/websock-webapp/echo"
            client.open(new URI( WebAppConstants.WEBSOCKET_FULL_URL ), new WebSocket() {
                @Override
                public void onOpen(WebSocket.Connection connection) {
                    System.out.println( "Client onOpen, sending: " + theLastReceivedMessage );
                    try {
                        connection.sendMessage( theLastReceivedMessage );
                    } catch (IOException e) {
                        System.err.println( "Failed to send message: "+e.getMessage() );
                        fail( "Got IOException" );
                    }
                }
                @Override
                public void onClose(int i, String s) {
                    System.out.println( "Client onClose" );
                }
        });
        WebSocket.Connection connection = connectionFuture.get(2, TimeUnit.SECONDS);
        assertThat(connection, is(notNullValue()));
        // close the connection
        connection.close();

        Thread.sleep(1000);
        assertThat( lastReceivedMessage, is( theLastReceivedMessage ) );
    }
}
