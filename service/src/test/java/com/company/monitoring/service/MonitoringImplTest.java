package com.company.monitoring.service;

import com.company.monitoring.service.fs.Dir;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringImplTest {

    public static final int VERIFY_TIMEOUT = 90000;
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
        doReturn(mock(WatchKey.class)).when(monitoring).register(anyString());

        Dir dir = mockDirCreation();
        when(dir.getPath()).thenReturn(temporaryFolderPath.toString());
        when(monitoring.isRunning()).thenReturn(true);
        monitoring.monitorDir(temporaryFolderPath.toString());
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

    @Test
    void submittedToFsChangesExecutorTest() {
        WatchKey watchKey = mock(WatchKey.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("path");
        when(watchKey.watchable()).thenReturn(path);

        when(watchService.poll()).thenReturn(watchKey).thenReturn(null);
        doNothing().when(monitoring).registerNow(any());

        when(fsChangesExecutor.submit(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Runnable runnable = invocationOnMock.getArgument(0);
                runnable.run();
                return mock(Future.class);
            }
        });
        monitoring.monitorDir("path");
        monitoring.start();

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(fsChangesExecutor, timeout(5000)).submit(argumentCaptor.capture());
        Runnable value = argumentCaptor.getValue();
        System.out.println(value);
    }

//    @Nested
//    class MoreReal {
//
//        @Test
//        void name() throws IOException {
//
//            monitoring.shutdown();
//
//            watchServiceExecutor = Executors.newSingleThreadExecutor();
//            WatchService watchService = FileSystems.getDefault().newWatchService();
//            fsChangesExecutor = Executors.newFixedThreadPool(3);
//            Handlers handlers = new Handlers();
//            monitoring = spy(new MonitoringImpl(watchServiceExecutor, watchService, fsChangesExecutor, handlers));
//
//
//            temporaryFolder.create();
//            File dir = temporaryFolder.newFolder("path");
//            String dirPath = dir.getPath();
//            TestHandler handler = spy(new TestHandler());
//
//            monitoring.assignHandler("xml", handler);
//            monitoring.monitorDir(dirPath);
//            monitoring.start();
//
//
//
//            await().pollDelay(10, TimeUnit.SECONDS).timeout(20, TimeUnit.SECONDS).until(() -> true);
//            Path file1 = Files.createFile(Paths.get(dirPath, "1.xml"));
//            Path file2 = Files.createFile(Paths.get(dirPath, "2.xml"));
//            Files.writeString(file1, "<test/>");
//
//
//            await().pollDelay(5, TimeUnit.SECONDS).until(() -> true);
//
//            verify(handler, timeout(VERIFY_TIMEOUT).times(1)).handle(ArgumentMatchers.argThat(file -> file.getName().equals("1.xml")));
//            monitoring.stop();
//            Files.writeString(file2, "<test/>");
//            verify(handler, timeout(VERIFY_TIMEOUT).times(0)).handle(ArgumentMatchers.argThat(file -> file.getName().equals("2.xml")));
//
//        }
//
//
//        @Test
//        void name2() throws IOException, InterruptedException {
//
//
//            monitoring.shutdown();
//
//            WatchService watchService = FileSystems.getDefault().newWatchService();
//            Handlers handlers = spy(new Handlers());
//            monitoring = spy(new MonitoringImpl(Executors.newSingleThreadExecutor(), watchService, Executors.newFixedThreadPool(3), handlers));
//
//
//            temporaryFolder.create();
//            File dir = temporaryFolder.newFolder("path");
//            String dirPath = dir.getPath();
//            TestHandler handler = spy(new TestHandler());
//
//            monitoring.assignHandler("xml", handler);
//            monitoring.monitorDir(dirPath);
//            monitoring.start();
////        await().pollDelay(10, TimeUnit.SECONDS).timeout(20, TimeUnit.SECONDS).until(() -> true);
//
//            System.out.println("creating file 3.xml");
//            Path file1 = Files.createFile(Paths.get(dirPath, "3.xml"));
//            System.out.println("created file 3.xml");
//            System.out.println("creating file 4.xml");
//            Path file2 = Files.createFile(Paths.get(dirPath, "4.xml"));
//            System.out.println("created file 4.xml");
//
//            System.out.println("writing file 3.xml");
//            Files.writeString(file1, "<test/>");
//            System.out.println("written file 3.xml");
//            Files.writeString(file1, "<test/>", StandardOpenOption.APPEND);
//
//            System.out.println("writing file 4.xml");
//            Files.writeString(file2, "<test/>");
//            System.out.println("written file 4.xml");
//
////        monitoring.stop();
//
//            ArgumentCaptor<com.company.monitoring.api.fs.File> captor = ArgumentCaptor.forClass(com.company.monitoring.api.fs.File.class);
//
//            verify(handler, timeout(VERIFY_TIMEOUT).times(2)).handle(captor.capture());
//
//            org.assertj.core.api.Assertions.assertThat(captor.getAllValues()).hasSize(2).extracting(f -> f.getName()).containsOnly("3.xml", "4.xml");
//
//        }
//
//    }


    private Dir mockDirCreation() {
        Dir dir = mock(Dir.class);
        when(monitoring.createDir(anyString())).thenAnswer((Answer<Dir>) invocationOnMock -> dir);
        return dir;
    }


    static class DirMatcher implements ArgumentMatcher<Dir> {

        private String path;

        private static DirMatcher byPath(String path) {
            DirMatcher dirMatcher = new DirMatcher();
            dirMatcher.path = path;
            return dirMatcher;
        }

        @Override
        public boolean matches(Dir dir) {
            return dir.getPath().equals(path);
        }
    }
}