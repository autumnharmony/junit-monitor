package com.company.monitoring.handlers;

import com.company.monitoring.api.Report;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.*;

@Slf4j
class FileReportConsumer implements Consumer<Report> {

    private final Path outputFilePath;
    private final Gson gson;

    FileReportConsumer(Path outputFilePath) {
        this.outputFilePath = outputFilePath;
        if (Files.exists(outputFilePath)) {
            // clear content
            try {
                Files.write(outputFilePath, "".getBytes(StandardCharsets.UTF_8), CREATE, TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        gson = new Gson();
    }

    @Override
    public void accept(Report report) {
        log.debug("report {}", report);
        try {
            Report reportAggr = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(outputFilePath))), Report.class);
            if (reportAggr == null) reportAggr = new Report();
            reportAggr.getTestSuites().addAll(report.getTestSuites());
            String json = gson.toJson(reportAggr);
            log.debug("json: {}", json);
            Files.write(outputFilePath, json.getBytes(StandardCharsets.UTF_8), CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (IOException e) {
            log.warn("Exception {}", e);
        }
    }
}
