package com.company.monitoring.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Test case info
 * Contains name and className
 */
@Data
@RequiredArgsConstructor
@EqualsAndHashCode
public class TestCase {
    private final String name;
    private final String className;
}
