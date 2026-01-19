package com.smarthostel.dto;

import jakarta.validation.constraints.Min;

public class AllocateRequest {
    @Min(1)
    private int students;

    private boolean needsAC;
    private boolean needsWashroom;

    public int getStudents() {
        return students;
    }

    public void setStudents(int students) {
        this.students = students;
    }

    public boolean isNeedsAC() {
        return needsAC;
    }

    public void setNeedsAC(boolean needsAC) {
        this.needsAC = needsAC;
    }

    public boolean isNeedsWashroom() {
        return needsWashroom;
    }

    public void setNeedsWashroom(boolean needsWashroom) {
        this.needsWashroom = needsWashroom;
    }
}

