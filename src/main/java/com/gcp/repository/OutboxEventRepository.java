package com.gcp.repository;

import com.gcp.enums.OutboxStatus;
import com.gcp.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEvent> findByStatusIn(List<OutboxStatus> statuses);

    Optional<OutboxEvent> findByEventId(String eventId);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) ORDER BY e.createdAt ASC")
    List<OutboxEvent> findReadyToProcess(@Param("status") OutboxStatus status, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.id = :id")
    Optional<OutboxEvent> findByIdForUpdate(@Param("id") Long id);
}
