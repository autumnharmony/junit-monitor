package com.company.monitoring.service;

import com.company.monitoring.service.fs.Dir;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringImplTest {

    MonitoringImpl monitoring;

    ExecutorService watchServiceExecutor;

    @Mock
    WatchService watchService;
    @Mock
    ExecutorService fsChangesExecutor;
    @Mock
    Handlers handlers;

    @BeforeEach
    void setUp() {
        watchServiceExecutor = Executors.newSingleThreadExecutor();
        monitoring = spy(new MonitoringImpl(watchServiceExecutor, watchService, fsChangesExecutor, handlers));
    }

    @AfterEach
    void tearDown() {

        monitoring.stop();
        monitoring.shutdown();
    }

    @Test
    void assignHandlerClassTest() {
        monitoring.assignHandler("xml", "com.company.monitoring.service.TestHandler");
        verify(handlers).put(eq("xml"), any(TestHandler.class));
    }

    @Test
    void assignHandlerInstanceTest() {
        TestHandler handler = new TestHandler();
        monitoring.assignHandler("xml", handler);
        verify(handlers).put(eq("xml"), eq(handler));
    }

    @Test
    void revokeHandlerTest() {
        monitoring.revokeHandler("xml");
        verify(handlers).remove("xml");
    }

    @Test
    void monitorDirIfNotRunningTest() {
        Dir dir = mockDirCreation();
        when(monitoring.isRunning()).thenReturn(false);

        monitoring.monitorDir("path");

        verify(monitoring).registerLater(dir);
    }

    @Test
    void monitorDirIfRunningTest(@TempDir Path temporaryFolderPath) throws IOException {
        when(monitoring.isRunning()).thenReturn(true);
        doReturn(mock(WatchKey.class)).when(monitoring).register(anyString());
        Dir dir = mockDirCreation(temporaryFolderPath.toString());
        monitoring.monitorDir(temporaryFolderPath.toString());

        verify(monitoring).registerNow(dir);
    }

    @Test
    void monitorNotExistingDirIfRunningTest(@TempDir Path temporaryFolderPath) throws IOException {
        when(monitoring.isRunning()).thenReturn(true);
        Path notExistingSubfolder = Paths.get(temporaryFolderPath.toString(), "notExistingSubfolder");
        Assertions.assertTrue(Files.notExists(notExistingSubfolder));
        doReturn(mock(WatchKey.class)).when(monitoring).register(anyString());
        Dir dir = mockDirCreation(notExistingSubfolder.toString());

        monitoring.monitorDir(temporaryFolderPath.toString());

        Assertions.assertTrue(Files.exists(notExistingSubfolder));
        verify(monitoring).registerNow(dir);
    }

    @Test
    void forgetDirTest() {
        Dir dir = mockDirCreation();
        monitoring.monitorDir("path");

        monitoring.forgetDir("path");

        verify(dir).setActive(false);
    }


    @Test
    void startTest() {
        monitoring.start();
    }

    @Test
    void stopTest() {
        monitoring.stop();
    }


    private Dir mockDirCreation() {
        Dir dir = mock(Dir.class);
        when(monitoring.createDir(anyString())).thenAnswer((Answer<Dir>) invocationOnMock -> dir);
        return dir;
    }


    private Dir mockDirCreation(String path) {
        Dir dir = mock(Dir.class);
        when(monitoring.createDir(anyString())).thenAnswer((Answer<Dir>) invocationOnMock -> dir);
        when(dir.getPath()).thenReturn(path.toString());
        return dir;
    }
}