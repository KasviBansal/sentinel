package com.sentinel.model;

public record Account(String number, String customer, boolean active) {
    @Override public String toString() { return customer + " · " + number; }
}
