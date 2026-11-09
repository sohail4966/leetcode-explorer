package org.example.service;

import org.example.config.SyncConfig;
import org.example.entity.SyncMetadata;
import org.example.repository.SyncMetadataRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final SyncConfig syncConfig;
    private final SyncMetadataRepository syncMetadataRepository;
    private final CompanyIngestionService companyIngestionService;

    public SyncService(SyncConfig syncConfig,
                       SyncMetadataRepository syncMetadataRepository,
                       CompanyIngestionService companyIngestionService) {
        this.syncConfig = syncConfig;
        this.syncMetadataRepository = syncMetadataRepository;
        this.companyIngestionService = companyIngestionService;
    }

    public SyncMetadata getStatus() {
        return syncMetadataRepository.findTopByOrderByIdDesc().orElse(null);
    }

    public void sync() {
        if (!syncConfig.isEnabled()) {
            log.info("Sync is disabled");
            return;
        }

        SyncMetadata metadata = syncMetadataRepository.findTopByOrderByIdDesc()
                .orElseGet(() -> {
                    SyncMetadata m = new SyncMetadata();
                    m.setStatus("PENDING");
                    return syncMetadataRepository.save(m);
                });

        if ("RUNNING".equals(metadata.getStatus())) {
            if (metadata.getLastSyncedAt() == null ||
                    metadata.getLastSyncedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
                log.warn("Stale RUNNING status detected, resetting");
                metadata.setStatus("FAILED");
                metadata.setErrorMessage("Reset: stale RUNNING status from previous crash");
                syncMetadataRepository.save(metadata);
            } else {
                log.info("Sync already in progress, skipping");
                return;
            }
        }

        Path repoDir = null;
        try {
            metadata.setStatus("RUNNING");
            metadata.setErrorMessage(null);
            syncMetadataRepository.save(metadata);

            repoDir = Files.createTempDirectory("leetcode-sync-");
            String latestSha = cloneAndGetSha(repoDir);

            if (latestSha.equals(metadata.getLastCommitSha())) {
                log.info("No new commits (SHA: {}), skipping sync", latestSha);
                metadata.setStatus("SUCCESS");
                syncMetadataRepository.save(metadata);
                return;
            }

            log.info("New commits detected. Previous: {}, Latest: {}", metadata.getLastCommitSha(), latestSha);
            ingestData(repoDir);

            metadata.setLastCommitSha(latestSha);
            metadata.setLastSyncedAt(LocalDateTime.now());
            metadata.setStatus("SUCCESS");
            syncMetadataRepository.save(metadata);
            log.info("Sync completed successfully");

        } catch (Exception e) {
            log.error("Sync failed", e);
            metadata.setStatus("FAILED");
            metadata.setErrorMessage(e.getMessage());
            syncMetadataRepository.save(metadata);
        } finally {
            if (repoDir != null) {
                deleteDirectory(repoDir);
            }
        }
    }

    private String cloneAndGetSha(Path targetDir) throws GitAPIException {
        log.info("Cloning repo to {}", targetDir);
        try (Git git = Git.cloneRepository()
                .setURI(syncConfig.getRepoUrl())
                .setBranch(syncConfig.getBranch())
                .setDirectory(targetDir.toFile())
                .setDepth(1)
                .call()) {
            ObjectId head = git.getRepository().resolve("HEAD");
            return head.getName();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve HEAD", e);
        }
    }

    private void ingestData(Path repoDir) throws IOException {
        try (Stream<Path> companyDirs = Files.list(repoDir)) {
            companyDirs
                    .filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(companyDir -> {
                        String companyName = companyDir.getFileName().toString();
                        companyIngestionService.ingestCompany(companyName, companyDir);
                    });
        }
    }

    private void deleteDirectory(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
