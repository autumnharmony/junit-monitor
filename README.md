# junit-monitor
If you need to do some stuff when some file is modified
In particular it can be used to handle junit reports

##service module

Main entry point

`com.company.monitoring.service.MonitoringMain`

SPI loading will load all declared implementations of `com.company.monitoring.api.Handler` from classpath
see module *handlers* for details

##client module
example of how to control running instance of monitoring service by RMI

#How to use
- build by mvn clean package (skip tests for now)
- run `com.company.monitoring.service.MonitoringMain` with handlers/target/handlers-1.0-SNAPSHOT.jar in classpath
- register some project's folder of surefire reports by invoking `com.company.monitoring.api.Monitoring.monitorDir`
- optional step register handler (OOB handler from handlers will be registered automatically if will be available in classpath)
- start monitoring by invoking `com.company.monitoring.api.Monitoring.start`
- run external project build to start generating reports

There is some real test available in test/src/test/java/org/sample/RealTest.java
See for details