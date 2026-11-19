package com.sentinel.model;

public enum RuleType {
    HIGH_AMOUNT("High Amount", "Flags transfers above the configured amount.", 50000, 30),
    FREQUENCY("Frequent Transactions", "Flags repeated outgoing transfers in a ten-minute window.", 5, 30),
    BLACKLIST("Blacklisted Account", "Flags a sender or receiver on the account blacklist.", 0, 50),
    UNUSUAL_TIME("Unusual Time", "Flags transfers during the overnight monitoring window.", 0, 20);

    public final String label;
    public final String description;
    public final long defaultThreshold;
    public final int weight;

    RuleType(String label, String description, long defaultThreshold, int weight) {
        this.label = label;
        this.description = description;
        this.defaultThreshold = defaultThreshold;
        this.weight = weight;
    }
}
