package com.company.monitoring.handlers;

import com.company.monitoring.api.Report;
import com.company.monitoring.api.TestCase;
import com.company.monitoring.api.TestSuite;
import com.company.monitoring.api.File;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class JunitTestReportHandlerTest {

    private static final String XML1 = "com/company/TEST-org.apache.spark.streaming.JavaDurationSuite.xml";
    private static final String XML2 = "com/company/TEST-org.apache.spark.streaming.JavaTimeSuite.xml";

    @Test
    public void testName() throws IOException, ParserConfigurationException, SAXException {

        byte[] bytes = getClass().getClassLoader().getResourceAsStream(XML1).readAllBytes();
        JunitTestReportHandler handler = new JunitTestReportHandler();
        List<Report> reports = new ArrayList<>();
        handler.setReportConsumer(report -> reports.add(report));
        handler.handle(new File(XML1, bytes, 0, "path"));

        Assertions.assertThat(reports).hasSize(1)
                .flatExtracting(x -> x.getTestSuites())
                .flatExtracting(x -> x.getTestCases())
                .extracting(tc -> tc.getName())
                .contains(
                        "testLess",
                        "testLessEq",
                        "testGreater",
                        "testGreaterEq",
                        "testPlus",
                        "testMinus",
                        "testTimes",
                        "testDiv",
                        "testMilliseconds",
                        "testSeconds",
                        "testMinutes"
                );
    }


    @Test
    public void testName2() throws IOException {

        JunitTestReportHandler handler = new JunitTestReportHandler();
        List<Report> reports = new ArrayList<>();
        handler.setReportConsumer(report -> reports.add(report));
        handler.handle(new File(XML1, getBytes(XML1), 0, "path"));
        handler.handle(new File(XML2, getBytes(XML2), 0, "path"));


        Assertions.assertThat(reports).hasSize(2)
                .flatExtracting(Report::getTestSuites)
                .flatExtracting(TestSuite::getTestCases)
                .extracting(TestCase::getName)
                .contains(
                        "testLess",
                        "testLessEq",
                        "testGreater",
                        "testGreaterEq",
                        "testPlus",
                        "testMinus",
                        "testTimes",
                        "testDiv",
                        "testMilliseconds",
                        "testSeconds",
                        "testMinutes",

                        "testLess",
                        "testLessEq",
                        "testGreater",
                        "testGreaterEq",
                        "testPlus",
                        "testMinusTime",
                        "testMinusDuration"
                );

        AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> extracting = Assertions.assertThat(reports)
                .flatExtracting(Report::getTestSuites)
                .flatExtracting(TestSuite::getTestCases)
                .extracting(TestCase::getClassName);
        extracting.filteredOn(className -> className.equals("org.apache.spark.streaming.JavaDurationSuite")).hasSize(11);
        extracting.filteredOn(className -> className.equals("org.apache.spark.streaming.JavaTimeSuite")).hasSize(7);

    }

    private byte[] getBytes(String file) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(file).readAllBytes();
    }
}