package com.company.monitoring.api;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * File
 */
@ToString(exclude = "content")
@EqualsAndHashCode
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

    /**
     * Path to file without filename
     */
    public String getPath() {
        return path;
    }

    /**
     * File name
     */
    public String getName() {
        return name;
    }

    /**
     * Content of file
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Checksum of file
     */
    public long getCheckSum() {
        return checkSum;
    }
}
