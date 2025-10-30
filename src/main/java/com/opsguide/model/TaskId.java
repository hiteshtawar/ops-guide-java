package com.opsguide.model;

public enum TaskId {
    CANCEL_ORDER("CANCEL_ORDER"),
    UPDATE_ORDER_STATUS("UPDATE_ORDER_STATUS"),
    CANCEL_CASE("CANCEL_CASE"),
    UPDATE_CASE_STATUS("UPDATE_CASE_STATUS"),
    UPDATE_SAMPLES("UPDATE_SAMPLES"),
    UPDATE_STAIN("UPDATE_STAIN"),
    GENERIC_OPERATION("GENERIC_OPERATION");
    
    private final String value;
    
    TaskId(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
