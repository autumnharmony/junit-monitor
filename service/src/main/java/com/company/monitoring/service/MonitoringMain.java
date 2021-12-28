package com.company.monitoring.service;

import com.company.monitoring.api.Monitoring;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MonitoringMain {


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
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind(Monitoring.SERVICE_NAME, stub);
    }
}
