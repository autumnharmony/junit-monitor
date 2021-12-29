package com.company.monitoring.handlers;

import com.company.monitoring.api.Report;
import com.company.monitoring.api.Configurable;
import com.company.monitoring.api.Handler;
import com.company.monitoring.api.File;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.util.function.Consumer;

@Slf4j
public class JunitTestReportHandler implements Handler<File>, Configurable, Serializable, Remote {

    private final SAXParserFactory factory;
    private Consumer<Report> reportConsumer = new DefaultReportConsumer();

    public JunitTestReportHandler() {
        factory = SAXParserFactory.newInstance();
    }

    @Override
    public void handle(File file) {
        log.debug("handle file {}", file);
        try {

            SAXParser saxParser = factory.newSAXParser();
            JunitSax saxHandler = new JunitSax();
            saxParser.parse(new ByteArrayInputStream(file.getContent()), saxHandler);
            Report report = saxHandler.getReport();
            log.debug("parsed pass to consumer {}", report);
            reportConsumer.accept(report);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.warn("Exception while handle file ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return "xml";
    }

    @Override
    public void configure(Object[] data) {
        if (data.length == 1) {
            Object object = data[0];
            if (object instanceof String) {
                String filePathString = (String) object;
                setReportConsumer(new FileReportConsumer(Paths.get(filePathString)));
            } else if (object instanceof Path) {
                setReportConsumer(new FileReportConsumer((Path) object));
            } else {
                // throw maybe
                log.warn("Cant configure, unexpected arguments");
            }
        } else {
            log.warn("Cant configure, unexpected arguments");
        }
    }

    public void setReportConsumer(Consumer<Report> reportConsumer) {
        this.reportConsumer = reportConsumer;
    }

    @Slf4j
    static class DefaultReportConsumer implements Consumer<Report> {
        @Override
        public void accept(Report report) {
            log.info(report.toString());
        }
    }
}
