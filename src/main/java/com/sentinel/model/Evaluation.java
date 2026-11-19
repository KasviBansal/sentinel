package com.sentinel.model;

import java.util.List;

public record Evaluation(int score, RiskLevel level, List<RuleMatch> matches) {
    public Evaluation {
        matches = List.copyOf(matches);
    }

    public boolean suspicious() { return score >= 30; }
    public String status() { return suspicious() ? "Suspicious" : "Normal"; }
}
