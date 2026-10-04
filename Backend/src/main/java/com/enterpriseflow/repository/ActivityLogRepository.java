package com.enterpriseflow.repository;

import com.enterpriseflow.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop20ByEntityTypeAndEntityIdInOrderByCreatedAtDesc(String entityType, java.util.Collection<UUID> entityIds);
    List<ActivityLog> findTop20ByActorEmailOrderByCreatedAtDesc(String email);

    List<ActivityLog> findTop20ByActorIdOrderByCreatedAtDesc(UUID actorId);

    List<ActivityLog> findTop20ByOrderByCreatedAtDesc();
}
