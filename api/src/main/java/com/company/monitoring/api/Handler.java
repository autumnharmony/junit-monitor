package com.company.monitoring.api;

/**
 * Handler
 * @param <T> data
 */
public interface Handler<T> {

    /**
     * Handle method
     * @param data which should be handled by that handler
     */
    void handle(T data);

    /**
     * Which file type can be processed by that handler
     * @return fileType (extension) e.g "xml"
     */
    String getType();
}
