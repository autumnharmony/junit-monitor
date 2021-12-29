package com.company.monitoring.service.task;

public interface Task<KEY> extends Runnable {
    KEY getKey();
}
