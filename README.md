websock-poc parent module. 

1.	Requires Java 7 and Wildfly 8 installed and JBOSS_HOME env variable set to the root folder of the Wildfly 8 installation. Arquillian picks
this location when running integration tests.

2.	Consists of two children modules :

	a.	/webapp (contains Web Sockets-related stuff, including src and websock-webapp.war final artifact and unit tests for the Server and Client End Points)
	b.	/wildfly (contains Wildfly-related stuff, including deployment of the websock-webapp.war into the Wildfly 8 AS and integration tests using Arquillian)

3.	Run with typical :

	$ mvn clean install

which should build everything, including running unit and integration tests. 
