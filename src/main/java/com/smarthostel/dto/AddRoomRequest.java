package com.smarthostel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AddRoomRequest {
    @NotBlank
    private String roomNo;

    @Min(1)
    private int capacity;

    private boolean hasAC;
    private boolean hasAttachedWashroom;

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isHasAC() {
        return hasAC;
    }

    public void setHasAC(boolean hasAC) {
        this.hasAC = hasAC;
    }

    public boolean isHasAttachedWashroom() {
        return hasAttachedWashroom;
    }

    public void setHasAttachedWashroom(boolean hasAttachedWashroom) {
        this.hasAttachedWashroom = hasAttachedWashroom;
    }
}

