package com.company.monitoring.service.fs;

import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ToString(exclude = "fileChecksums")
public class Dir {

    private final String path;
    private boolean active = true;

    private Map<String, Long> fileChecksums = new ConcurrentHashMap<>();

    public boolean isActive() {
        return active;
    }

    public Dir(String path) {
        this.path = path;
    }

    public Map<String, Long> getFileChecksums() {
        return fileChecksums;
    }

    public String getPath() {
        return path;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
