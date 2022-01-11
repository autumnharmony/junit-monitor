package com.company.monitoring.service;

import com.company.monitoring.api.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoreRealTest {

    public static final int VERIFY_TIMEOUT = 10000;
    private MonitoringImpl monitoring;

    @BeforeEach
    void setUp() throws IOException {
        MonitoringImpl realMonitoring = new MonitoringImpl();

        Handlers handlers = spy(new Handlers());
        realMonitoring.setHandlers(handlers);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        realMonitoring.setWatchService(watchService);
        monitoring = spy(realMonitoring);
    }

    @AfterEach
    void tearDown() {
        monitoring.stop();
        monitoring.shutdown();
    }

    @Test
    void twoFilesCreatedSecondWillNotBeProcessedCauseMonitoringStopped(@TempDir Path dir) throws IOException {
        String dirPath = dir.toString();
        TestHandler handler = spy(new TestHandler());

        monitoring.assignHandler("xml", handler);
        monitoring.monitorDir(dirPath);
        monitoring.start();

        Path file1 = Files.createFile(Paths.get(dirPath, "1.xml"));
        Path file2 = Files.createFile(Paths.get(dirPath, "2.xml"));
        Files.writeString(file1, "<test/>");

        await().pollDelay(5, TimeUnit.SECONDS).until(() -> true);
        verify(handler, timeout(VERIFY_TIMEOUT).times(1)).handle(ArgumentMatchers.argThat(file -> file.getName().equals("1.xml")));
        monitoring.stop();
        Files.writeString(file2, "<test/>");
        verify(handler, timeout(VERIFY_TIMEOUT).times(0)).handle(ArgumentMatchers.argThat(file -> file.getName().equals("2.xml")));
    }


    @Test
    void twoFilesCreatedBothWillBeProcessed(@TempDir Path dir) throws IOException, InterruptedException {
        String dirPath = dir.toString();
        TestHandler handler = spy(new TestHandler());

        Set<File> arguments = ConcurrentHashMap.newKeySet();
        doAnswer(invocationOnMock -> {
            arguments.add(invocationOnMock.getArgument(0));
            return null;
        }).when(handler).handle(nullable(File.class));

        monitoring.assignHandler("xml", handler);
        monitoring.monitorDir(dirPath);
        monitoring.start();

        System.out.println("creating file 3.xml");
        Path file1 = Files.createFile(Paths.get(dirPath, "3.xml"));
        System.out.println("created file 3.xml");
        System.out.println("creating file 4.xml");
        Path file2 = Files.createFile(Paths.get(dirPath, "4.xml"));
        System.out.println("created file 4.xml");

        System.out.println("writing file 3.xml");
        Files.writeString(file1, "<test/>");
        System.out.println("written file 3.xml");

        System.out.println("writing file 4.xml");
        Files.writeString(file2, "<test/>");
        System.out.println("written file 4.xml");

        await().atMost(20, TimeUnit.SECONDS).until(() -> arguments.size() >= 2);
        org.assertj.core.api.Assertions.assertThat(arguments).hasSize(2).extracting(f -> f.getName()).containsOnly("3.xml", "4.xml");
    }


    @Test
    void dirDeletedWhileProcessing(@TempDir Path tempDir) throws IOException {
        Path dir = Files.createDirectory(Paths.get(tempDir.toString(), "subfolder"));

        Handlers handlers = spy(new Handlers());
        MonitoringImpl monitoringReal = new MonitoringImpl();
        monitoringReal.setHandlers(handlers);
        monitoringReal.setWatchService(FileSystems.getDefault().newWatchService());
        MonitoringImpl monitoring = spy(monitoringReal);

        String dirPath = dir.toString();
        TestHandler handler = spy(new TestHandler());

        monitoring.assignHandler("xml", handler);
        monitoring.monitorDir(dirPath);
        monitoring.start();


        Files.deleteIfExists(dir);
        await().timeout(5, TimeUnit.SECONDS).until(() -> Files.exists(dir));
    }
}

