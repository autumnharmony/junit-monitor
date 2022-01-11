package com.company.monitoring.service.task.impl;

import com.company.monitoring.service.task.Task;
import com.company.monitoring.service.task.TaskExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("rawtypes")
@Slf4j
public class TaskExecutorImpl<Key> implements TaskExecutor<Key> {

    private final ExecutorService executorService;
    private final Map<Key, Future> futures;

    private final ReentrantLock submitTaskLock = new ReentrantLock();


    public TaskExecutorImpl() {
        executorService = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(String.format("%s-%s", "TaskExecutorImpl", UUID.randomUUID()));
            return thread;
        });
        futures = new ConcurrentHashMap<>();
    }

    public TaskExecutorImpl(ExecutorService executorService, Map<Key, Future> futures) {
        this.executorService = executorService;
        this.futures = futures;
    }

    @Override
    public void submitTask(Task<Key> task) {
        submitTaskLock.lock();
        try {
            Key key = task.getKey();
            log.info("submitTask " + key);

            if (futures.containsKey(key)) {
                log.info("already contains such key {}", key);
                cancelTask(key);
            }

            Future<?> submit = executorService.submit(task);
            futures.put(key, submit);
        }
        finally {
            submitTaskLock.unlock();
        }
    }

    @Override
    public void cancelTask(Key key) {
        log.info("cancel task {}", key);
        Future future = futures.get(key);
        if (future != null) {
            future.cancel(true);
        } else {
            log.warn("can't cancel task with key {}, not found", key);
        }
    }

    @Override
    public void shutdown() {
        futures.values().forEach(future -> future.cancel(true));
    }
}
