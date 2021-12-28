package com.company.monitoring.service.task;


public interface TaskExecutor<KEY> {

    /**
     * Submit new task, if there is a task with such key, it will be canceled
     * @param task task to execute
     *
     */
    void submitTask(Task<KEY> task);

    /**
     * Cancel task by given key
     * @param key
     */
    void cancelTask(KEY key);

    void shutdown();
}
