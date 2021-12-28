package com.company.monitoring.service.task.impl;

import com.company.monitoring.service.task.Task;
import com.company.monitoring.service.task.impl.TaskExecutorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExecutorImplTest {

    public static final String KEY = "key1";
    @Mock
    ExecutorService executorService;

    Map<String, Future> futures = new ConcurrentHashMap<>();
    @Mock
    Logger logger;


    private TaskExecutorImpl<String> taskExecutor;

    @BeforeEach
    void setUp() {
        taskExecutor = spy(new TaskExecutorImpl<>(executorService, futures));
    }

    class TestTask implements Task<String> {

        final String key;
        final Runnable runnable;

        public TestTask(String key, Runnable runnable) {
            this.key = key;
            this.runnable = runnable;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

    @Test
    void submitTaskTest() {
        TestTask task = new TestTask(KEY, () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        when(executorService.submit(any(Runnable.class))).thenReturn(mock(Future.class));
        taskExecutor.submitTask(task);
        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    void resubmitTaskTest() {
        TestTask task1 = new TestTask(KEY, () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        TestTask task2 = new TestTask(KEY, () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Future mock1 = mock(Future.class);
        Future mock2 = mock(Future.class);

        when(executorService.submit(any(Runnable.class))).thenReturn(mock1).thenReturn(mock2);
        taskExecutor.submitTask(task1);
        taskExecutor.submitTask(task2);
        verify(executorService).submit(task1);
        verify(taskExecutor).cancelTask(KEY);
        verify(executorService).submit(task2);
    }

    @Test
    void cancelTask() {
        TestTask task1 = new TestTask(KEY, () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Future mock1 = mock(Future.class);

        when(executorService.submit(any(Runnable.class))).thenReturn(mock1);

        taskExecutor.submitTask(task1);
        taskExecutor.cancelTask(KEY);

    }


    @Test
    void cancelNotExistingTask() {
        taskExecutor.cancelTask(KEY);
    }
}