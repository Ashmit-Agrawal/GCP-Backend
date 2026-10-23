package com.gcp.service;

import com.gcp.model.AuditLog;
import com.gcp.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /**
     * Appends one entry to the audit trail. Entries are never edited or deleted.
     */
    public void log(String actor, String actionType, String entityType, Object entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setActionType(actionType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId != null ? String.valueOf(entityId) : null);
        entry.setDetails(details);
        entry.setLoggedAt(LocalDateTime.now());
        repo.save(entry);
    }

    public List<AuditLog> find(String entityType, String entityId) {
        if (entityType != null && entityId != null) {
            return repo.findByEntityTypeAndEntityIdOrderByLoggedAtAsc(entityType, entityId);
        }
        return repo.findAllByOrderByLoggedAtDesc();
    }
}
