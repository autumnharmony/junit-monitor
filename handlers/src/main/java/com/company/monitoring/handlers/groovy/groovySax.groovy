//package com.company.monitoring.handlers.groovy
//
//
//import com.company.monitoring.api.fs.File
//import com.company.monitoring.company.handlers.Report
//import com.company.monitoring.company.handlers.TestSuite
//import com.company.monitoring.handlers.JunitSax
//import com.company.monitoring.handlers.Report
//import com.company.monitoring.handlers.TestCase
//import com.company.monitoring.handlers.TestSuite
//import org.xml.sax.Attributes
//import org.xml.sax.SAXException
//import org.xml.sax.helpers.DefaultHandler
//
//import javax.xml.parsers.ParserConfigurationException
//import javax.xml.parsers.SAXParser
//import javax.xml.parsers.SAXParserFactory
//import java.util.function.Consumer
//
//class GroovyJunitSax extends DefaultHandler {
//
//    private StringBuilder currentValue = new StringBuilder();
//
//
//    public Report getReport() {
//        return report;
//    }
//
//
//    Report report = new Report();
//    TestSuite currentTestSuite;
//    TestCase currentTestCase;
//
//    @Override
//    void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//        // reset the tag value
//        currentValue.setLength(0);
//
//
//        System.out.printf("Start Element : %s%n", qName);
//
//        if (qName.equalsIgnoreCase("testsuite")) {
//            // get tag's attribute by name
//            String name = attributes.getValue("name");
//
//            currentTestSuite = new TestSuite(name);
//
//            report.getTestSuites().add(currentTestSuite);
//            System.out.printf("%s testsuite", name);
//        }
//
//        if (qName.equalsIgnoreCase("testcase")) {
//            // get tag's attribute by index, 0 = first attribute
//            String name = attributes.getValue("name");
//            String classname = attributes.getValue("classname");
//            currentTestCase = new TestCase(name, classname);
//            System.out.printf("%s testcase", name);
//
//            if (currentTestSuite != null && currentTestCase != null) {
//                currentTestSuite.getTestCases().add(currentTestCase);
//            }
//
//        }
//    }
//
//    @Override
//    void endElement(String uri, String localName, String qName) throws SAXException {
//        System.out.printf("End Element : %s%n", qName);
//
//        if (qName.equalsIgnoreCase("testsuite")) {
//            currentTestSuite = null;
//        }
//
//        if (qName.equalsIgnoreCase("testcase")) {
//            currentTestCase = null;
//
//        }
//    }
//
//    @Override
//    void characters(char[] ch, int start, int length) throws SAXException {
//
//        // The characters() method can be called multiple times for a single text node.
//        // Some values may missing if assign to a new string
//
//        // avoid doing this
//        // value = new String(ch, start, length);
//
//        // better append it, works for single or multiple calls
//        currentValue.append(ch, start, length);
//
//    }
//
//}
//
//class JunitTestReportHandler {
//    class DefaultReportConsumer implements Consumer<Report> {
//        @Override
//        public void accept(Report report) {
//            System.out.println(report);
//        }
//    }
//
//    private final SAXParserFactory factory;
//    private Consumer<Report> reportConsumer = new JunitTestReportHandler.DefaultReportConsumer();
//
//    public JunitTestReportHandler() {
//        factory = SAXParserFactory.newInstance();
//    }
//
//    public void setReportConsumer(Consumer<Report> reportConsumer) {
//        this.reportConsumer = reportConsumer;
//    }
//
//    @Override
//    public void handle(File data) {
//        try {
//            SAXParser saxParser = factory.newSAXParser();
//            JunitSax saxHandler = new JunitSax();
//
//            saxParser.parse(new ByteArrayInputStream(data.getContent()), saxHandler);
//            reportConsumer.accept(saxHandler.getReport());
//        } catch (SAXException | IOException | ParserConfigurationException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
//
////new SAXParser();