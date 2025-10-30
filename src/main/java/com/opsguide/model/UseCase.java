package com.opsguide.model;

public enum UseCase {
    OPERATIONAL_ASK("U2");
    
    private final String value;
    
    UseCase(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
