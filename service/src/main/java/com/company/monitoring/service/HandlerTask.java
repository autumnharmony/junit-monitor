package com.company.monitoring.service;

import com.company.monitoring.api.Handler;
import com.company.monitoring.service.task.Task;

public abstract class HandlerTask<T, KEY> implements Task<KEY> {

    private Handler<T> handler;
    private T data;

    public Handler<T> getHandler() {
        return handler;
    }

    public T getData() {
        return data;
    }

    public HandlerTask(Handler<T> handler, T data) {
        this.handler = handler;
        this.data = data;
    }

    @Override
    public void run() {
        handler.handle(data);
    }
}
