package org.example.dto;

import java.time.LocalDateTime;

public record SyncStatusResponse(
        String lastCommitSha,
        LocalDateTime lastSyncedAt,
        String status,
        String errorMessage
) {}
