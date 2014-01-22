package com.websockpoc.jetty;

import com.websockpoc.common.WebAppConstants;
import com.websockpoc.common.WebSocketClientEndpoint;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Arquillian integration test suppossed to deploy ../webapp/target/websock-webapp.war 
 * (built by the sibling /websock-poc/webapp mvn module) into Jetty 9 and assert
 * correct Web Socket behavior. 
 * 
 * IMPORTANT: Currently, is broken possibly due to the fact that Jetty 9 Embedded Arquillian Container 
 * was built against Jetty 9.0.3.v20130506 see
 *      https://github.com/arquillian/arquillian-container-jetty/blob/master/jetty-embedded-9/pom.xml
 * which DOES NOT have full/complete Web Sockets Support (although Jetty 9.1.1 jars are on the classpath). 
 * Installing Standalone Jetty 9.0.3.v20130506 and manually dropping websock-webapp.war into into its /webapps folder
 * exhibits same behavior when tested from Firefox (ie. jquery.gracefulWebSocket.js displays
 * "Firefox can't establish a connection to the server at ws://127.0.0.1:8080/websock-webapp/echo."
 * with its code attempting to use fallback method (since Http was not correctly upgraded to WebSocket
 * as is the expected behavior).
 * 
 * @author kloyevsk
 */

@RunWith(Arquillian.class)
public class WebsockJettyDeploymentWithArquillianTest_Failing
{
    /**
     * Arquillian WebArchive deployment method. Works with :
     * 1.   /websock-poc/jetty/pom.xml arquillian-jetty-9-embedded mvn profile  
     * 2.   /websock-poc/jetty/src/test/resources/arquillian.xml Arquillian configuration for Jetty
     * 
     * Does the following :
     * 1.   Grabs the ../webapp/target/websock-webapp.war (built as part of the /websock-poc/webapp sub-project)
     * so make sure to run "mvn install" on the /websock-poc/webapp sub-project before executing this)
     * 2.   Starts up Embedded Jetty 9 
     * 3.   Deploys ../webapp/target/websock-webapp.war into Jetty 9.
     * 
     * @return
     * @throws IOException 
     */
    @OverProtocol("Servlet 3.0")
    @Deployment
    public static WebArchive createTestArchive() throws IOException {

        System.out.println( "-->>> createTestArchive() START" );
        
        // To load it up from the file system, ie. to import (ie. to do the reverse operation) so 
        // This is how to use an existing .war on the local file system and convert it into the WebArchive
        // object which is installed into JBoss as an .war by Arquillian.   
        // websock-webapp.war
        String warWithSuffix = WebAppConstants.WEBSOCKET_WEBAPP_NAME + ".war";
        WebArchive webArchive = ShrinkWrap
            .create( ZipImporter.class, warWithSuffix )
            .importFrom( new File( ".", "../webapp/target/" + warWithSuffix ) )
            .as( WebArchive.class );
        
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

        ClientContainer jettyWebSocketContainer = getWebSocketsContainer(); 
        assertNotNull( "Jetty WebSockets Container is null", jettyWebSocketContainer );
        
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
        Session clientSession = jettyWebSocketContainer.connectToServer( WebSocketClientEndpoint.class,
            // "ws://localhost:8080/websock-webapp/echo"
            new URI( WebAppConstants.WEBSOCKET_FULL_URL ) );
            
        // ... (which also creates a matching Server EndPoint session as well)
        serverEndpointOpenSessions = getOpenServerSessions();
        assertTrue( "No configured open session or more than one", serverEndpointOpenSessions != null && serverEndpointOpenSessions.size() == 1 );

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
