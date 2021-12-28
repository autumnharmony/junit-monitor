package com.company.monitoring.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode
public class Report {
    private final Set<TestSuite> testSuites = new LinkedHashSet<>();
}
