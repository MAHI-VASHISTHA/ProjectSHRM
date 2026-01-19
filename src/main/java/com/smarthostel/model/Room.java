package com.smarthostel.model;

public class Room {
    private String roomNo;
    private int capacity;
    private boolean hasAC;
    private boolean hasAttachedWashroom;

    public Room() {
        // For Jackson
    }

    public Room(String roomNo, int capacity, boolean hasAC, boolean hasAttachedWashroom) {
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.hasAC = hasAC;
        this.hasAttachedWashroom = hasAttachedWashroom;
    }

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

