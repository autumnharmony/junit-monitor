package com.company.monitoring.api;

public interface Handler<T> {

    void handle(T data);

    String getType();

}
