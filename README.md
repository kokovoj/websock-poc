websock-poc parent module. 

1.	Requires Java 7 and Wildfly 8 installed and JBOSS_HOME env variable set to the root folder of the Wildfly 8 installation. Arquillian picks
this location when running integration tests.

2.	Consists of two children modules :

	a.	/webapp (contains Web Sockets-related stuff, including src and websock-webapp.war final artifact and unit tests for the Server and Client End Points)
	b.	/wildfly (contains Wildfly-related stuff, including deployment of the websock-webapp.war into the Wildfly 8 AS and integration tests using Arquillian)

3.	Run with typical :

	$ mvn clean install

which should build everything, including running unit and server-side integration tests.

4.	To deploy the websock-webapp.war and test via UI

	a.	compile /webapp sub-module that builds /webapp/target/websock-webapp.war

	$ cd webapp
	$ mvn clean install

	b.	start your favorite Servlet Container (Undertow/Wildfly, Jetty, Tomcat, etc.)

	c.	copy /webapp/target/websock-webapp.war into Servlet Container's deployment folder; confirm via logs that it has been deployed

	d.	open your favorite browser and navigate to http://localhost:8080/websock-webapp

	e.	enter text into textfield and press Send Web Socket Data button; observe that server returns same message you entered
		appended with "_server"	 
