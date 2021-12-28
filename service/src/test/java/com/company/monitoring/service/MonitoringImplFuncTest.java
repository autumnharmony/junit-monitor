package com.company.monitoring.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;


public class MonitoringImplFuncTest {

    private MonitoringImpl monitoring;
    private StringBuilder stringBuilder;

    @BeforeEach
    void setUp() {
        monitoring = new MonitoringImpl();
        monitoring.assignHandler("xml", TestHandler.class.getCanonicalName());
        stringBuilder = new StringBuilder();
        TestHandler.setStringBuilder(stringBuilder);
    }

    @Test
    void name(@TempDir Path dirPath) throws IOException {

        monitoring.monitorDir(dirPath.toString());
        monitoring.start();

        Path path = Paths.get(dirPath.toString(), "1.xml");
        Path file = Files.createFile(path);
        String fileContent = "<test></test>";
        Files.writeString(file, fileContent);


        await().atMost(5, TimeUnit.SECONDS).until(() -> !stringBuilder.toString().isEmpty());
        Assertions.assertEquals(fileContent, stringBuilder.toString());

    }


}
