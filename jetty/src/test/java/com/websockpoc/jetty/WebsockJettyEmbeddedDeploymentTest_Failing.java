package com.websockpoc.jetty;

import com.websockpoc.common.WebAppConstants;
import com.websockpoc.common.WebSocketClientEndpoint;
import java.io.File;
import java.net.URI;
import java.util.Set;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Jetty 9 embedded integration test suppossed to deploy ../webapp/target/websock-webapp.war 
 * (built by the sibling /websock-poc/webapp mvn module) into Jetty 9 and assert
 * correct Web Socket behavior. 
 *  
 * IMPORTANT: Currently, is broken.
 * 
 * @author kloyevsk
 */

public class WebsockJettyEmbeddedDeploymentTest_Failing
{
    private static Server server = null;
    private static ServerContainer webSocketServerContainer = null;
    
    @BeforeClass
    public static void startEmbeddedJetty_9_1_1() throws Exception {
        // Bootstrap and start Jetty 9.1.1 Container
        server = new Server( 8080 );
        
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath( "/websock-webapp" );
        //But the .war should already have WebSocketServerEndpoint.class in it        
        webapp.setWar( new File( ".", "../webapp/target/" + WebAppConstants.WEBSOCKET_WEBAPP_NAME + ".war" ).getPath() );
        server.setHandler( webapp );
        // Initialize the WebSocket Server Container
        WebSocketServerContainerInitializer.configureContext( webapp ); 
        server.start();
        
        // After Jetty started and WebSocket Server Container was initialized, obtain it
        webSocketServerContainer = ( ServerContainer ) webapp.getServletContext().getAttribute( 
            javax.websocket.server.ServerContainer.class.getName() );
        assertNotNull( webSocketServerContainer );
        System.out.println( "--->> webSocketServerContainer [" + webSocketServerContainer + "] of class [" + 
            ( webSocketServerContainer == null ? null : webSocketServerContainer.getClass().getName() ) + "]" );
        
        //This should really NOT be needed since WebSocketServerEndpoint is in the .war and should be
        //picked up via annotation. PLUS this requires refactoring WebSocketServerEndpoint from out of the
        // /webapp child module into /common which is really unnecessary at this point.
        //  webSocketServerContainer.addEndpoint( WebSocketServerEndpoint.class );
        
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        assertNotNull( "Jetty WebSockets Container is null", container );
        container.connectToServer( WebSocketClientEndpoint.class,
//            // "ws://localhost:8080/websock-webapp/echo"
           new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        
/* This still throws the same damn, exception
        
Caused by: org.eclipse.jetty.websocket.api.UpgradeException: Didn't switch protocols
	at org.eclipse.jetty.websocket.client.io.UpgradeConnection.validateResponse(UpgradeConnection.java:272)
	at org.eclipse.jetty.websocket.client.io.UpgradeConnection.read(UpgradeConnection.java:203)
	at org.eclipse.jetty.websocket.client.io.UpgradeConnection.onFillable(UpgradeConnection.java:148)        
*/                
              
        /* 
            Running server as standalone and client as standalone via Spring work though.
        
        final ServletHolder servletHolder = new ServletHolder( new DefaultServlet() );
        final ServletContextHandler context = new ServletContextHandler();

        context.setContextPath( "/" );
        context.addServlet( servletHolder, "/*" );
        context.addEventListener( new ContextLoaderListener() );   
        context.setInitParameter( "contextClass", AnnotationConfigWebApplicationContext.class.getName() );
        context.setInitParameter( "contextConfigLocation", AppConfig.class.getName() );
        */
        
    }
        
    @AfterClass
    public static void stopEmbeddedJetty_9_1_1() throws Exception {
        if( server != null ) server.stop();
    }
    
    @After
    public void after(){
        // Nullify static variables after each test, in preparation for next test
        WebSocketClientEndpoint.lastClientMessageReceived = null;
    }
    
    @Test
    public void testWebSocketMessageInitiatedByClientEndPoint() throws Exception {
        
        System.out.println("--> testWebSocketMessageInitiatedByClientEndPoint START ");

        
        /*(
        ClientContainer jettyWebSocketContainer = getWebSocketsContainer(); 
        assertNotNull( "Jetty WebSockets Container is null", jettyWebSocketContainer );
        
        //jettyWebSocketContainer.
        
        // Initially no Client Endpoint session, so there is no matching Server Endpoint session either
        Set<Session> serverEndpointOpenSessions = getOpenServerSessions();
        assertTrue( "Initially Server Endpoint Session is not null", serverEndpointOpenSessions == null || serverEndpointOpenSessions.isEmpty() );        
        
// KL : Latest : 
//        
//        Session clientSession = jettyWebSocketContainer.connectToServer( WebSocketClientEndpoint.class,
//            // "ws://localhost:8080/websock-webapp/echo"
//            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
//        
//      line is causing grief. Despite the fact that jettyWebSocketContainer is NOT null as asserted above, the instance of the
//      (annotated server endpoint) WebServerEndPoint class is not getting deployed / registered into it when trying to deploy 
//      into a Jetty 9 Embedded Arquillian Container Adapter (when manually deploying into the Standalone Jetty 9.1.1 everything
//      works correctly). The exception is :        
//               
//        java.io.IOException: Connect failure
//	at org.eclipse.jetty.websocket.jsr356.ClientContainer.connect(ClientContainer.java:154)
//	at org.eclipse.jetty.websocket.jsr356.ClientContainer.connectToServer(ClientContainer.java:170)
//	at com.websockpoc.jetty.WebsockJettyDeploymentTest.testWebSocketMessageInitiatedByClientEndPoint(WebsockJettyDeploymentTest.java:102)
//        ...
//        Caused by: org.eclipse.jetty.websocket.api.UpgradeException: Didn't switch protocols
//	at org.eclipse.jetty.websocket.client.io.UpgradeConnection.validateResponse(UpgradeConnection.java:272)
//	at org.eclipse.jetty.websocket.client.io.UpgradeConnection.read(UpgradeConnection.java:203)
//	at org.eclipse.jetty.websocket.client.io.UpgradeConnection.onFillable(UpgradeConnection.java:148)
           
        
        
        // Create a new Client EndPoint session ... 
        Session clientSession = webSocketServerContainer.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
            
        // ... (which also creates a matching Server EndPoint session as well)
        serverEndpointOpenSessions = getOpenServerSessions();
        assertTrue( "No configured open session or more than one", serverEndpointOpenSessions != null && serverEndpointOpenSessions.size() == 1 );
        
        */
                
        System.out.println("--> testWebSocketMessageInitiatedByClientEndPoint END ");
    }
    
    // Actual class returned is org.eclipse.jetty.websocket.jsr356.ClientContainer
    private ClientContainer getWebSocketsContainer(){
        return ( ClientContainer ) ContainerProvider.getWebSocketContainer(); 
    }
    
    // Returns the Open Sessison for Server Endpoint (which should be
    // WebSocketServerEndpoint's annotated with @ServerEndpoint("/echo") )
    private Set<Session> getOpenServerSessions(){
        ClientContainer jettySocketServer = getWebSocketsContainer();
        return jettySocketServer.getOpenSessions();
    }
}
