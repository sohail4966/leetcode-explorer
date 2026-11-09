package org.example.controller;

import org.example.dto.SyncStatusResponse;
import org.example.entity.SyncMetadata;
import org.example.service.SyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/status")
    public ResponseEntity<SyncStatusResponse> getStatus() {
        SyncMetadata metadata = syncService.getStatus();
        if (metadata == null) {
            return ResponseEntity.ok(new SyncStatusResponse(null, null, "NEVER_RUN", null));
        }
        return ResponseEntity.ok(new SyncStatusResponse(
                metadata.getLastCommitSha(),
                metadata.getLastSyncedAt(),
                metadata.getStatus(),
                metadata.getErrorMessage()
        ));
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerSync() {
        new Thread(() -> syncService.sync()).start();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Sync triggered"));
    }
}
