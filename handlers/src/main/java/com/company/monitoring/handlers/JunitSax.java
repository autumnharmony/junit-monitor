package com.company.monitoring.handlers;

import com.company.monitoring.api.Report;
import com.company.monitoring.api.TestCase;
import com.company.monitoring.api.TestSuite;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class JunitSax extends DefaultHandler {

    private StringBuilder currentValue = new StringBuilder();


    public Report getReport() {
        return report;
    }


    Report report = new Report();
    TestSuite currentTestSuite;
    TestCase currentTestCase;

    @Override
    public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes) {

        // reset the tag value
        currentValue.setLength(0);


        System.out.printf("Start Element : %s%n", qName);

        if (qName.equalsIgnoreCase("testsuite")) {
            // get tag's attribute by name
            String name = attributes.getValue("name");

            currentTestSuite = new TestSuite(name);

            report.getTestSuites().add(currentTestSuite);
            System.out.printf("%s testsuite", name);
        }

        if (qName.equalsIgnoreCase("testcase")) {
            // get tag's attribute by index, 0 = first attribute
            String name = attributes.getValue("name");
            String classname = attributes.getValue("classname");
            currentTestCase = new TestCase(name, classname);
            System.out.printf("%s testcase", name);

            if (currentTestSuite != null && currentTestCase != null) {
                currentTestSuite.getTestCases().add(currentTestCase);
            }

        }

    }

    @Override
    public void endElement(String uri,
                           String localName,
                           String qName) {

        System.out.printf("End Element : %s%n", qName);

        if (qName.equalsIgnoreCase("testsuite")) {
            currentTestSuite = null;
        }

        if (qName.equalsIgnoreCase("testcase")) {
            currentTestCase = null;

        }
//
//        if (qName.equalsIgnoreCase("salary")) {
//            System.out.printf("Salary : %s%n", currentValue.toString());
//        }
//
//        if (qName.equalsIgnoreCase("bio")) {
//            System.out.printf("Bio : %s%n", currentValue.toString());
//        }

    }

    // http://www.saxproject.org/apidoc/org/xml/sax/ContentHandler.html#characters%28char%5B%5D,%20int,%20int%29
    // SAX parsers may return all contiguous character data in a single chunk,
    // or they may split it into several chunks
    @Override
    public void characters(char ch[], int start, int length) {

        // The characters() method can be called multiple times for a single text node.
        // Some values may missing if assign to a new string

        // avoid doing this
        // value = new String(ch, start, length);

        // better append it, works for single or multiple calls
        currentValue.append(ch, start, length);

    }

}