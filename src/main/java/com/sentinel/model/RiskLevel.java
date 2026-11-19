package com.sentinel.model;

public enum RiskLevel {
    Low, Medium, High, Critical;

    public static RiskLevel fromScore(int score) {
        if (score >= 80) return Critical;
        if (score >= 60) return High;
        if (score >= 30) return Medium;
        return Low;
    }
}
