package com.websockpoc.wildfly;

import com.websockpoc.webapp.WebSocketClientEndpoint;
import com.websockpoc.webapp.WebAppConstants;

import io.undertow.websockets.jsr.ConfiguredServerEndpoint;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import javax.websocket.ContainerProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.websocket.Session;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Arquillian integration test deploying ../webapp/target/websock-webapp.war 
 * (built by the sibling /websock-poc/webapp mvn module) into Wildfly 8 and asserting 
 * correct Web Socket behavior.
 *
 * @author kloyevsk
 */
@RunWith(Arquillian.class)
//Not using Spring yet so this is not necessary yet
//  @SpringWebConfiguration(servletName = "employee")
public class WebsockWildflyDeploymentWithArquillianTest {
              
    /**
     * Arquillian WebArchive deployment method. Works with :
     * 1.   /websock-poc/wildfly/pom.xml arquillian-wildfly-8-managed mvn profile  
     * 2.   /websock-poc/wildfly/src/test/resources/arquillian.xml Arquillian configuration specifying the version of Wildfly to run
     * 
     * Does the following :
     * 1.   Grabs the ../webapp/target/websock-webapp.war (built as part of the /websock-poc/webapp sub-project)
     * so make sure to run "mvn install" on the /websock-poc/webapp sub-project before executing this)
     * 2.   Starts up Wildfly 8
     * 3.   Deploys ../webapp/target/websock-webapp.war into Wildfly 8.
     * 
     * @return
     * @throws IOException 
     */
    @Deployment
    @OverProtocol("Servlet 3.0")
    public static WebArchive createTestArchive() throws IOException {
                
                
        // To load it up from the file system, ie. to import (ie. to do the reverse operation) so 
        // This is how to use an existing .war on the local file system and convert it into the WebArchive
        // object which is installed into JBoss as an .war by Arquillian.   
        // websock-webapp.war
        String warWithSuffix = WebAppConstants.WEBSOCKET_WEBAPP_NAME + ".war";
        WebArchive webArchive = ShrinkWrap
            .create( ZipImporter.class, warWithSuffix )
            .importFrom( new File( ".", "../webapp/target/" + warWithSuffix ) )
            .as( WebArchive.class );
        
        // IMPORTANT: programmatically add /websock-poc/webapp/src/test/java/com/websockpoc/webapp/WebSocketClientEndpoint.java
        // (matching test WebSocketClientEndpoint implementation to the 
        // /websock-poc/webapp/src/main/java/com/websockpoc/webapp/WebSocketServerEndpoint.java) into the deployed 
        // websock-webapp.war (which does not have it), so that both Server and Client WebSocket endpoints are deployed.
        webArchive.addClass( WebSocketClientEndpoint.class );
        
        System.out.println( "==> createTestArchive is about to deploy [" + warWithSuffix + "] with its contents being [\n" + 
            webArchive.toString( true ) + "\n]\n" );

        return webArchive;
    }
    
    @After
    public void after(){
        // Nullify static variables after each test, in preparation for next test
        WebSocketClientEndpoint.lastClientMessageReceived = null;
    }
    
    @Test
    public void testWebSocketMessageInitiatedByClientEndPoint() throws Exception {
        
        System.out.println("--> testWebSocketMessageInitiatedByClientEndPoint START ");
        
        // container is an instance of io.undertow.websockets.jsr.ServerWebSocketContainer
        // which is Undertow's Web Sockets spec implementation. Since Arquillian allows
        // this test method to run in Wildfly's JVM (not this test method is NOT annotated
        // with @RunAsClient; if it were would get a RuntimeException 
        //    java.lang.RuntimeException: Could not find an implementation class.
        //    at javax.websocket.ContainerProvider.getWebSocketContainer(ContainerProvider.java:73)
        //    at com.websockpoc.wildfly.WebsockWildflyDeployment.testWildflyDeployedWebsocket(WebsockWildflyDeployment.java:116)
        // ).
        ServerWebSocketContainer undertowWebSocketContainer = getWebSocketsContainer(); 
        assertNotNull( "Wildfly WebSockets Container is null", undertowWebSocketContainer );
        
        // Initially no Client Endpoint session, so there is no matching Server Endpoint session either
        ConfiguredServerEndpoint configuredServerEndpoint = getConfiguredServerEndpoint();
        assertTrue( 
            configuredServerEndpoint.getEndpointConfiguration().getEndpointClass().getName().endsWith( 
                "WebSocketServerEndpoint" ) && 
            // "/echo" 
            WebAppConstants.WEBSOCKET_SERVER_ENDPOINT_URL.equals( configuredServerEndpoint.getEndpointConfiguration().getPath() ) );
        Set<Session> openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "Initially Server Endpoint Session is not null", openSessions == null || openSessions.isEmpty() );
                
        // Create a new Client EndPoint session ... 
        Session clientSession = undertowWebSocketContainer.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        // ... (which also creates a matching Server EndPoint session as well)
        configuredServerEndpoint = getConfiguredServerEndpoint();
        openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "No configured open session or more than one", openSessions != null && openSessions.size() == 1 );

        //
        // ClientEndpoint sends message to ServerEndpoint
        //
        String clientInitiatedMessage = "clientInitiatedMessage_1";
        clientSession.getBasicRemote().sendText( clientInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ClientEndpoint received ( clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX)
        // back from the ServerEndpoint
        assertTrue( ( clientInitiatedMessage + WebAppConstants.SERVER_PROCESSED_MESSAGE_SUFFIX ).equals( WebSocketClientEndpoint.lastClientMessageReceived ) );
                        
        // Since server sent back (clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX) 
        // to client, client terminated the session.
        assertFalse( clientSession.isOpen() );
        
        // 
        // Since client session was terminated, need to start a new one before ClientEndpoint sending
        // message to ServerEndpoint second time
        //
        clientSession = undertowWebSocketContainer.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        
        clientInitiatedMessage = "clientInitiatedMessage_2";
        clientSession.getBasicRemote().sendText( clientInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ClientEndpoint received ( clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX)
        // back from the ServerEndpoint
        assertTrue( ( clientInitiatedMessage + WebAppConstants.SERVER_PROCESSED_MESSAGE_SUFFIX).equals( WebSocketClientEndpoint.lastClientMessageReceived ) );
        
        // Since server sent back (clientInitiatedMessage + WebSocketServerEndpoint.SERVER_PROCESSED_MESSAGE_SUFFIX) 
        // to client, client terminated the session.
        assertFalse( clientSession.isOpen() );
        
        // TODO : need to also assert ServerEndpoint (similar to how they are done in
        //      /websock-poc/webapp/src/test/java/com/websockpoc/webapp/WebSocketServerEndpointTest.java
        // ), but can't do it easily just yet (since need to first obtain/inject WebSocketClientEndpoint instance
        // deployed into Wildfly via @Inject, Spring, JNDI or some other way). It should be doable since
        // this test method runs within the same JVM as Wildfly.        
        
        System.out.println("--> testWebSocketMessageInitiatedByClientEndPoint END ");
    }
    
    
    @Test
    public void testWebSocketMessageInitiatedByServerEndPoint() throws Exception {
        
        System.out.println( "-- testWebSocketMessageInitiatedByServerEndPoint START --" );
        
        assertTrue( WebSocketClientEndpoint.lastClientMessageReceived == null );
        
        // container is an instance of io.undertow.websockets.jsr.ServerWebSocketContainer
        // which is Undertow's Web Sockets spec implementation. Since Arquillian allows
        // this test method to run in Wildfly's JVM (not this test method is NOT annotated
        // with @RunAsClient; if it were would get a RuntimeException 
        //    java.lang.RuntimeException: Could not find an implementation class.
        //    at javax.websocket.ContainerProvider.getWebSocketContainer(ContainerProvider.java:73)
        //    at com.websockpoc.wildfly.WebsockWildflyDeployment.testWildflyDeployedWebsocket(WebsockWildflyDeployment.java:116)
        // ).
        ServerWebSocketContainer undertowWebSocketContainer = getWebSocketsContainer(); 
        assertNotNull( "Wildfly WebSockets Container is null", undertowWebSocketContainer );
        
        // Initially no Client Endpoint session, so there is no matching Server Endpoint session either
        ConfiguredServerEndpoint configuredServerEndpoint = getConfiguredServerEndpoint();
        assertTrue( 
            configuredServerEndpoint.getEndpointConfiguration().getEndpointClass().getName().endsWith( 
                "WebSocketServerEndpoint" ) && 
            // "/echo" 
            WebAppConstants.WEBSOCKET_SERVER_ENDPOINT_URL.equals( configuredServerEndpoint.getEndpointConfiguration().getPath() ) );
        Set<Session> openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "Initially Server Endpoint Session is not null", openSessions == null || openSessions.isEmpty() );
                
        // Create a new Client EndPoint session ... 
        undertowWebSocketContainer.connectToServer( WebSocketClientEndpoint.class, 
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        // ... (which also creates a matching Server EndPoint session as well)
        configuredServerEndpoint = getConfiguredServerEndpoint();
        openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "No configured open session or more than one", openSessions != null && openSessions.size() == 1 );
        Session serverSession = openSessions.iterator().next();
        
        //
        // After serverSession is initiated, ServerEndpoint sends message to ClientEndpoint ...
        //
        String serverInitiatedMessage = "serverInitiatedMessage_1";
        serverSession.getBasicRemote().sendText( serverInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ClientEndpoint received serverInitiatedMessage as-is
        assertTrue( serverInitiatedMessage.equals( WebSocketClientEndpoint.lastClientMessageReceived ) );

        // Since client sent back (serverInitiatedMessage + WebSocketClientEndpoint.CLIENT_PROCESSED_MESSAGE_SUFFIX) 
        // to server, server terminated the session 
        assertFalse( serverSession.isOpen() );
        configuredServerEndpoint = getConfiguredServerEndpoint();
        openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "Open Server Endpoint sessions when there should be none", openSessions == null || openSessions.isEmpty() );

        //
        // Since server session was terminated, need to start a new one before ServerEndpoint sends message 
        // to ClientEndpoint second time. Can create a new Server this by creating a new ***Client Session*** !!!
        //
        undertowWebSocketContainer.connectToServer( WebSocketClientEndpoint.class, 
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
        // ... (which also creates a matching Server EndPoint session as well)
        configuredServerEndpoint = getConfiguredServerEndpoint();
        openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "No configured open session or more than one", openSessions != null && openSessions.size() == 1 );
        serverSession = openSessions.iterator().next();
        serverInitiatedMessage = "serverInitiatedMessage_2";
        
        serverSession.getBasicRemote().sendText( serverInitiatedMessage );
        
        Thread.sleep( 1000 );
        
        // Assert that ClientEndpoint received serverInitiatedMessage as-is
        assertTrue( serverInitiatedMessage.equals( WebSocketClientEndpoint.lastClientMessageReceived ) );

        // Since client sent back (serverInitiatedMessage + WebSocketClientEndpoint.CLIENT_PROCESSED_MESSAGE_SUFFIX) 
        // to server, server terminated the session 
        assertFalse( serverSession.isOpen() );
        configuredServerEndpoint = getConfiguredServerEndpoint();
        openSessions = configuredServerEndpoint.getOpenSessions();
        assertTrue( "Open Server Endpoint sessions when there should be none", openSessions == null || openSessions.isEmpty() );        
        
        System.out.println( "-- testWebSocketMessageInitiatedByServerEndPoint END --" );
    }    
    
    // Actual class returned is io.undertow.websockets.jsr.ServerWebSocketContainer
    // (Undertow is Wildfly's Web Server and Web Sockets Container) 
    private ServerWebSocketContainer /* WebSocketContainer */ getWebSocketsContainer(){
        return ( ServerWebSocketContainer ) ContainerProvider.getWebSocketContainer(); 
    }
    
    // Returns the Configured Server Endpoint (which should be
    // WebSocketServerEndpoint's annotated with @ServerEndpoint("/echo") )
    private ConfiguredServerEndpoint getConfiguredServerEndpoint(){
        ServerWebSocketContainer undertowSocketServer = getWebSocketsContainer();
        List<ConfiguredServerEndpoint> cses = undertowSocketServer.getConfiguredServerEndpoints();
        if( cses != null && cses.size() == 1 ){
            return cses.get( 0 );
        }
        else {
            return null;
        }        
    }
}
