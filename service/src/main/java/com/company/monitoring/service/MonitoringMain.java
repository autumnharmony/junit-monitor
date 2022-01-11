package com.company.monitoring.service;

import com.company.monitoring.api.Monitoring;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

@Slf4j
public class MonitoringMain {

    public static final int DEFAULT_PORT = 1099;

    public static void main(String[] args) {
        try {
            new MonitoringMain().run(args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void run(String[] args) throws RemoteException {
        Monitoring server = new MonitoringImpl();
        Monitoring stub = (Monitoring) UnicastRemoteObject
                .exportObject(server, 0);
        int port;
        try {
            port = Integer.parseInt(System.getProperty("com.company.monitoring.port", Integer.toString(DEFAULT_PORT)));
            log.info("will use port {}", port);
        } catch (Exception e) {
            port = DEFAULT_PORT;
            log.info("will use default port {}", port);
        }
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind(Monitoring.SERVICE_NAME, stub);
    }
}
