package com.company.monitoring.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Utils {

    public static String getExtension(String filename) {
        String[] split = filename.split("\\.");
        if (split.length > 0) {
            return split[split.length - 1];
        }
        return null;
    }

    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }
}
