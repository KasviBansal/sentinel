package com.sentinel.model;

public record Customer(long id, String name, String email, String phone, String account, String city, boolean active) {
    public String displayId() { return "CUS-%04d".formatted(id); }
}
