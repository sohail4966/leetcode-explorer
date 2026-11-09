package org.example.repository;

import org.example.entity.SyncMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncMetadataRepository extends JpaRepository<SyncMetadata, Long> {
    Optional<SyncMetadata> findTopByOrderByIdDesc();
}
