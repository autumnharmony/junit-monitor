package com.company.monitoring;

import com.company.monitoring.api.Monitoring;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MonitoringClient {

    public static void main(String[] args) throws Exception {

        Registry registry = LocateRegistry.getRegistry();
        Monitoring monitoring = (Monitoring) registry
                .lookup(Monitoring.SERVICE_NAME);

        monitoring.assignHandler("xml", "com.company.monitoring.handlers.JunitTestReportHandler");
        monitoring.monitorDir("/home/anton/spark-master/streaming/target/surefire-reports");
        monitoring.start();

//        monitoring.forgetDir("/home/anton/spark-master/streaming/target/surefire-reports");
//        monitoring.shutdown();
    }


}
