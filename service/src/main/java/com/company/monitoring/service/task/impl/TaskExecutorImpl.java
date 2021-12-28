package com.company.monitoring.service.task.impl;

import com.company.monitoring.service.task.Task;
import com.company.monitoring.service.task.TaskExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TaskExecutorImpl<Key> implements TaskExecutor<Key> {

    private final ExecutorService executorService;
    private final Map<Key, Future> futures;

    private ReentrantLock submitTaskLock = new ReentrantLock();


    public TaskExecutorImpl() {
        executorService = Executors.newCachedThreadPool();
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
                log.info("already contains such key");
                cancelTask(key);
            }

            Future<?> submit = executorService.submit(task);
            futures.put(key, submit);
        }
        finally {
            submitTaskLock.unlock();
        }
    }

//    private Task wrapLogging(Task<Key> task) {
//        return new Task() {
//            @Override
//            public Object getKey() {
//                return task.getKey();
//            }
//
//            @Override
//            public void run() {
//                logger.info("Start task " + task.getKey());
//                task.run();
//                logger.info("Finish task " + task.getKey());
//
//            }
//        };
//    }

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
