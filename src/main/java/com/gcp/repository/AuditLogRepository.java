package com.gcp.repository;

import com.gcp.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByLoggedAtAsc(String entityType, String entityId);
    List<AuditLog> findAllByOrderByLoggedAtDesc();
}
