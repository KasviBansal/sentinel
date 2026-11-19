package com.sentinel.model;

public enum AlertStatus {
    NEW("New"), UNDER_REVIEW("Under Review"), RESOLVED("Resolved");

    private final String label;
    AlertStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
