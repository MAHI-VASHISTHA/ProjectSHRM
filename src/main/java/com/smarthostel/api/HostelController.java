package com.smarthostel.api;

import com.smarthostel.dto.AddRoomRequest;
import com.smarthostel.dto.AllocateRequest;
import com.smarthostel.model.Room;
import com.smarthostel.service.HostelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class HostelController {
    private final HostelService hostelService;

    public HostelController(HostelService hostelService) {
        this.hostelService = hostelService;
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> addRoom(@Valid @RequestBody AddRoomRequest req) {
        boolean ok = hostelService.addRoom(req.getRoomNo(), req.getCapacity(), req.isHasAC(), req.isHasAttachedWashroom());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Room number already exists (or invalid)."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Room added."));
    }

    @GetMapping("/rooms")
    public List<Room> listRooms() {
        return hostelService.getAllRooms();
    }

    @GetMapping("/rooms/search")
    public List<Room> searchRooms(
            @RequestParam(defaultValue = "1") @Min(1) int minCapacity,
            @RequestParam(defaultValue = "false") boolean needsAC,
            @RequestParam(defaultValue = "false") boolean needsWashroom
    ) {
        return hostelService.searchRooms(minCapacity, needsAC, needsWashroom);
    }

    @PostMapping("/rooms/allocate")
    public ResponseEntity<?> allocate(@Valid @RequestBody AllocateRequest req) {
        return hostelService.allocateRoom(req.getStudents(), req.isNeedsAC(), req.isNeedsWashroom())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No room available")));
    }
}

