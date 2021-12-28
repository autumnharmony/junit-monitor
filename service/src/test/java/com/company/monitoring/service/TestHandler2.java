package com.company.monitoring.service;

import com.company.monitoring.api.Handler;
import com.company.monitoring.api.File;

public class TestHandler2 implements Handler {

    private static StringBuilder stringBuilder;

    static void setStringBuilder(StringBuilder stringBuilder) {
        TestHandler2.stringBuilder = stringBuilder;
    }

    @Override
    public void handle(Object data) {

        if (data instanceof File) {
            File file = (File) data;
            String content = new String(file.getContent());
            System.out.println(content);
            stringBuilder.append(content);
        }
    }

    @Override
    public String getType() {
        return "xml";
    }
}
