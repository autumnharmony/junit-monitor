package com.company.monitoring.service;

import com.company.monitoring.api.Handler;
import com.company.monitoring.api.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestHandler implements Handler<File> {

    private static StringBuilder stringBuilder;

    static void setStringBuilder(StringBuilder stringBuilder) {
        TestHandler.stringBuilder = stringBuilder;
    }

    @Override
    public void handle(File data) {
        log.debug("handle {}", data);
        if (data != null) {
            String content = new String(data.getContent());
            System.out.println(content);
            stringBuilder.append(content);
        }
    }

    @Override
    public String getType() {
        return "xml";
    }
}
