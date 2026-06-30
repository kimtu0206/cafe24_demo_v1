package org.example.cafe24_demo_v1.shared.application;

public record SyncResult(int processedCount, int failedCount, int apiFailureCount, String errorMessage) {}
