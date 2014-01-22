websock-poc parent module. 

1.	Requires Java 7 and Wildfly 8 installed and JBOSS_HOME env variable set to the root folder of the Wildfly 8 installation. Arquillian picks
        this location when running integration tests.

2.	Consists of these children modules :

        a.      /common (contains common classes used in other modules, such as constants file and a sample ClientEndPoint implementation)
	b.	/webapp (contains Web Sockets Web Application (producing websock-webapp.war as its final artifact) and unit tests for the Server and Client End Points)
	c.	/wildfly (contains Wildfly 8-related stuff, including deployment of the websock-webapp.war into it and integration tests using Arquillian)
        d.	/jetty (contains Jetty 9-related stuff, including deployment of the websock-webapp.war into it and integration tests using Arquillian)

3.	Build and run unit and integration tests with typical :

	$ mvn clean install

        which should build everything, including running unit and server-side integration tests.

4.	To deploy the websock-webapp.war and manually test via browser 

	a.	build websock-webapp.war, start Servlet Container and deploy websock-webapp.war into the Container

       		i.	(fast method) use below handy either :
            
                    I.  WildFly Maven Plugin one liner (which downloads, extracts, and installs Wildfly 8 for you the very first time
                    you run below command under /webapp/target/wildfly-run/wildfly-8.0.0.CR1) which builds /webapp/target/websock-webapp.war, 
                    starts up Wildfly 8, and deploys /webapp/target/websock-webapp.war into it

                    $ cd webapp
                    $ mvn -Dmaven.test.skip=true wildfly:run -Dwildfly.version=8.0.0.CR1 

                    OR

                    II. Jetty Maven Plugin one liner (which downloads, extracts, and installs Jetty 9 for you the very first time
                    you run below command which builds /webapp/target/websock-webapp.war, starts up Jetty 9, and deploys 
                    /webapp/target/websock-webapp.war into it

                    $ cd webapp
                    $ mvn -Dmaven.test.skip=true jetty:run-war 

		OR
		
		ii.	(slower method)	

			1.	compile /webapp sub-module that builds /webapp/target/websock-webapp.war

			$ cd webapp
			$ mvn clean install

			2.	start your favorite Servlet Container (Undertow/Wildfly, Jetty, Tomcat, etc.)

			3.	copy /webapp/target/websock-webapp.war into Servlet Container's deployment folder; confirm via logs that it has been deployed

	b.	test via browser

		i.	open your favorite browser and navigate to http://127.0.0.1:8080/websock-webapp

		ii.	enter text into textfield and press Send Web Socket Data button; observe that server returns same message you entered
		appended with "_server"	(on the line after Received from server:) below textfield for each text you entered 
