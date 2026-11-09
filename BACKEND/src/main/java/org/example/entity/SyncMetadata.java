package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_metadata")
public class SyncMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "last_commit_sha", length = 40)
    private String lastCommitSha;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLastCommitSha() { return lastCommitSha; }
    public void setLastCommitSha(String lastCommitSha) { this.lastCommitSha = lastCommitSha; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
