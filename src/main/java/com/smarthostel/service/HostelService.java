package com.smarthostel.service;

import com.smarthostel.model.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HostelService {
    private final List<Room> rooms = new ArrayList<>();

    public HostelService() {
        // Preload sample data (mirrors your Swing app)
        addRoom("101", 1, true, true);
        addRoom("102", 2, false, true);
        addRoom("103", 4, true, false);
        addRoom("104", 2, true, true);
        addRoom("201", 6, false, false);
    }

    public synchronized boolean addRoom(String roomNo, int capacity, boolean ac, boolean washroom) {
        String normalized = normalizeRoomNo(roomNo);
        if (normalized.isEmpty()) {
            return false;
        }
        for (Room r : rooms) {
            if (normalizeRoomNo(r.getRoomNo()).equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        rooms.add(new Room(roomNo.trim(), capacity, ac, washroom));
        return true;
    }

    public synchronized List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public synchronized List<Room> searchRooms(int minCapacity, boolean requireAC, boolean requireWashroom) {
        return rooms.stream()
                .filter(r -> r.getCapacity() >= minCapacity)
                .filter(r -> !requireAC || r.isHasAC())
                .filter(r -> !requireWashroom || r.isHasAttachedWashroom())
                .sorted(Comparator.comparingInt(Room::getCapacity).thenComparing(Room::getRoomNo))
                .collect(Collectors.toList());
    }

    public synchronized Optional<Room> allocateRoom(int students, boolean needsAC, boolean needsWashroom) {
        List<Room> candidates = searchRooms(students, needsAC, needsWashroom);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        candidates.sort(Comparator.comparingInt(Room::getCapacity));
        return Optional.of(candidates.get(0));
    }

    private String normalizeRoomNo(String roomNo) {
        return roomNo == null ? "" : roomNo.trim().toLowerCase(Locale.ROOT);
    }
}

