package org.example.worker;

import org.example.config.SyncConfig;
import org.example.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class GitHubSyncWorker {

    private static final Logger log = LoggerFactory.getLogger(GitHubSyncWorker.class);

    private final SyncService syncService;
    private final SyncConfig syncConfig;

    public GitHubSyncWorker(SyncService syncService, SyncConfig syncConfig) {
        this.syncService = syncService;
        this.syncConfig = syncConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!syncConfig.isEnabled()) return;
        var status = syncService.getStatus();
        if (status == null || status.getLastCommitSha() == null) {
            log.info("First run detected, triggering initial sync");
            syncService.sync();
        }
    }

    @Scheduled(fixedDelayString = "#{${sync.interval-hours:6} * 3600000}",
               initialDelayString = "#{${sync.interval-hours:6} * 3600000}")
    public void scheduledSync() {
        log.info("Scheduled sync triggered");
        syncService.sync();
    }
}
