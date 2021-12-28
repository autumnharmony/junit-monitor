package com.company.monitoring.api;

import com.company.monitoring.api.TestCase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode
public class TestSuite {
    private final String name;
    private Set<TestCase> testCases = new LinkedHashSet<>();
}