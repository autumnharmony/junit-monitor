package com.company.monitoring.service;

import com.company.monitoring.api.File;
import com.company.monitoring.api.Handler;
import com.company.monitoring.service.fs.Dir;
import com.company.monitoring.service.task.TaskExecutor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlersTest {

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private Handlers handlers;


    @Test
    void putTest() {
        Handler<File> expected = mock(Handler.class);
        when(expected.getType()).thenReturn("xml");
        handlers.put(expected);
        Handler actual = handlers.get("xml");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void putTest2() {
        handlers.put("xml", new TestHandler());
        Assertions.assertInstanceOf(TestHandler.class, handlers.get("xml"));
    }

    @Test
    void putExistsTest() {
        Handler<File> expected = mock(Handler.class);
        when(expected.getType()).thenReturn("xml");

        Handler<File> expected2 = mock(Handler.class);
        when(expected2.getType()).thenReturn("xml");
        handlers.put(expected);
        handlers.put(expected2);
        Handler actual = handlers.get("xml");
        Assertions.assertEquals(expected2, actual);
    }

    @Test
    void putExistsTest3() {
        TestHandler expected = new TestHandler();
        TestHandler expected2 = new TestHandler();
        handlers.put("xml", expected);
        handlers.put("xml", expected2);
        Assertions.assertEquals(expected2, handlers.get("xml"));
    }

    @Test
    void removeTest() {
        handlers.put("xml", new TestHandler());
        Assertions.assertInstanceOf(TestHandler.class, handlers.get("xml"));
        handlers.remove("xml");
        Assertions.assertNull(handlers.get("xml"));
    }

    @Test
    void removeNotExistingTest() {
        handlers.remove("xml");
    }

    @Test
    void submitTaskOnFileModificationTest(@TempDir Path dirPath) throws IOException {
        handlers.put("xml", new TestHandler());
        Dir dir = mock(Dir.class);
        when(dir.getPath()).thenReturn(dirPath.toString());


        String fileName = "file.xml";
        Path filePath = Paths.get(dirPath.toString(), fileName);
        Path xmlFile = Files.createFile(filePath);
        Files.writeString(xmlFile, "<test/>", StandardOpenOption.APPEND);

        WatchEvent watchEvent = mock(WatchEvent.class);
        when(watchEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_MODIFY);
        when(watchEvent.context()).thenReturn(fileName);
        handlers.handleEvents(dir, dirPath, Collections.singletonList(watchEvent));

        // todo
//        verify(taskExecutor).submitTask(any(Task.class), eq(filePath.toString()));
    }

    @Test
    void doNotSubmitTaskOnSameContentTest(@TempDir Path dirPath) throws IOException {

        handlers.put("xml", new TestHandler());
        Dir dir = spy(new Dir(dirPath.toString()));

        String fileName = "file.xml";
        Path filePath = Paths.get(dirPath.toString(), fileName);
        Path xmlFile = Files.createFile(filePath);
        Files.writeString(xmlFile, "<test/>", StandardOpenOption.WRITE);

        WatchEvent watchEvent = mock(WatchEvent.class);
        when(watchEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_MODIFY);
        when(watchEvent.context()).thenReturn(fileName);


        handlers.handleEvents(dir, dirPath, Collections.singletonList(watchEvent));

        Files.writeString(xmlFile, "<test/>", StandardOpenOption.WRITE);
        handlers.handleEvents(dir, dirPath, Collections.singletonList(watchEvent));

        // todo
//        verify(taskExecutor, times(1)).submitTask(any(Task.class), eq(filePath.toString()));
    }

    @Test
    void shutdownTest() {
        handlers.shutdown();
        verify(taskExecutor).shutdown();
    }
}