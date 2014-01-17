POC for Web Sockets on Wildfly 8 AS running within Undertow Web Server

1. To deploy the application (and to download, install and start Wildfly 8) use

$ mvn wildfly:run -Dwildfly.version=8.0.0.CR1

command. 

Notes :
a. Downloading and installation of Wildfly will happen only once (the very first time)
and Wildfly will be installed to ./target/wildfly-run/wildfly-8.0.0.CR1/ directory.
b. wildfly.version command-line parameter value specifies the version of Wildfly to download and install. 



