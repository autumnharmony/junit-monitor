package com.company.monitoring.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Monitoring system
 * Will monitor added folders
 * Once some file in monitoring folder is changed it will be processed by registered handler
 */
public interface Monitoring extends Remote {

    String SERVICE_NAME = "MonitoringService";

    /**
     * Add directory for monitoring
     * If monitoring is not in running state, it will be added right after starting
     * @param path path to directory
     * @throws RemoteException
     */
    void monitorDir(String path) throws RemoteException;

    /**
     * Remove directory from monitoring
     * All files in according directory which will be created or modified after invocation will be ignored
     * @param path
     * @throws RemoteException
     */
    void forgetDir(String path) throws RemoteException;

    /**
     * Shutdown all monitoring without caring about not processed yet files
     * @throws RemoteException
     */
    void shutdown() throws RemoteException;

    /**
     * Stop all monitoring, all created but not processed files will be processed
     * @throws RemoteException
     */
    void stop() throws RemoteException;

    /**
     * Start monitoring directories which already added by {@link Monitoring#monitorDir(java.lang.String)}
     * New directories can be added also after start method
     * @throws RemoteException
     */
    void start() throws RemoteException;

    /**
     * Assign handler to type
     * Handler is mapped to file extension
     * {@see com.company.JunitTestReportHandler}
     * @param type file extension
     * @param clazz handler class
     * @throws RemoteException
     */
    void assignHandler(String type, String clazz) throws RemoteException;

    /**
     * Assign handler to type
     * Handler is mapped to file extension
     * {@see com.company.JunitTestReportHandler}
     * @param type file extension
     * @param clazz handler class
     * @param extra extra params to init handler
     * @throws RemoteException
     */
    void assignHandler(String type, String clazz, Object[] extra) throws RemoteException;

    /**
     * Assign handler to type
     * Handler is mapped to file extension
     * {@see com.company.JunitTestReportHandler}
     * @param type file extension
     * @param handler handler class
     * @throws RemoteException
     */
    void assignHandler(String type, Handler handler) throws RemoteException;

    /**
     * Revoke handler
     * @param type file extension
     * @throws RemoteException
     */
    void revokeHandler(String type) throws RemoteException;

    /**
     * Get handler by type
     * @param type
     * @return
     * @throws RemoteException
     */
    Handler getHandler(String type) throws RemoteException;

    void configureHandler(String type, Object[] objects) throws RemoteException;
}
