package com.company.monitoring.api;

import lombok.ToString;

@ToString(exclude = "content")
public class File {
    private String path;
    private String name;
    private byte[] content;
    private long checkSum;

    public File(String name, byte[] content, long checkSum, String path) {
        this.path = path;
        this.name = name;
        this.content = content;
        this.checkSum = checkSum;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public long getCheckSum() {
        return checkSum;
    }
}
