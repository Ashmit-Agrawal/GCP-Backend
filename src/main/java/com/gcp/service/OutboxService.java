package com.gcp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.enums.OutboxStatus;
import com.gcp.enums.ParcelStatus;
import com.gcp.model.LandParcel;
import com.gcp.model.OutboxEvent;
import com.gcp.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxRepo;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepo,
                         MongoTemplate mongoTemplate,
                         ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists an event in the PostgreSQL outbox table within the caller's active database transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent recordEvent(String eventType, String aggregateType, String aggregateId, Object payload) {
        String jsonPayload;
        try {
            jsonPayload = payload instanceof String ? (String) payload : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(jsonPayload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(5)
                .createdAt(LocalDateTime.now())
                .build();

        return outboxRepo.save(event);
    }

    /**
     * Scheduled processor to consume pending outbox events and synchronize with MongoDB.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> readyEvents = outboxRepo.findReadyToProcess(OutboxStatus.PENDING, now);
        for (OutboxEvent event : readyEvents) {
            try {
                processSingleEvent(event.getId());
            } catch (Exception e) {
                log.error("Error dispatching outbox event {}", event.getId(), e);
            }
        }
    }

    @Transactional
    public void processSingleEvent(Long eventId) {
        OutboxEvent event = outboxRepo.findByIdForUpdate(eventId).orElse(null);
        if (event == null || event.getStatus() == OutboxStatus.COMPLETED) {
            return;
        }

        event.setStatus(OutboxStatus.PROCESSING);
        outboxRepo.saveAndFlush(event);

        try {
            dispatchToMongoDB(event);
            event.setStatus(OutboxStatus.COMPLETED);
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);
            log.info("Successfully processed outbox event {}: {} on {}", event.getEventId(), event.getEventType(), event.getAggregateId());
        } catch (Exception ex) {
            log.error("Failed to process outbox event {}: {}", event.getEventId(), ex.getMessage());
            int newRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(newRetryCount);
            event.setLastError(ex.getMessage());
            if (newRetryCount >= event.getMaxRetries()) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} permanently failed after {} attempts", event.getEventId(), newRetryCount);
            } else {
                event.setStatus(OutboxStatus.PENDING);
                long backoffSeconds = (long) Math.pow(2, newRetryCount) * 2;
                event.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            }
        }
        outboxRepo.save(event);
    }

    private void dispatchToMongoDB(OutboxEvent event) throws Exception {
        Map<String, Object> data = objectMapper.readValue(event.getPayload(), Map.class);
        String parcelId = event.getAggregateId();

        switch (event.getEventType()) {
            case "PARCEL_ALLOCATED" -> {
                Long companyId = data.get("companyId") != null ? Long.valueOf(data.get("companyId").toString()) : null;
                String dateStr = (String) data.get("allotmentDate");
                LocalDate allotmentDate = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

                Query query = new Query(Criteria.where("_id").is(parcelId));
                Update update = new Update()
                        .set("status", ParcelStatus.ALLOTTED.name())
                        .set("allottedCompanyId", companyId)
                        .set("allotmentDate", allotmentDate);

                mongoTemplate.upsert(query, update, LandParcel.class);
            }
            case "PARCEL_RELEASED" -> {
                Query query = new Query(Criteria.where("_id").is(parcelId));
                Update update = new Update()
                        .set("status", ParcelStatus.AVAILABLE.name())
                        .set("allottedCompanyId", null)
                        .set("allotmentDate", null);

                mongoTemplate.updateFirst(query, update, LandParcel.class);
            }
            default -> log.warn("Unrecognized outbox event type: {}", event.getEventType());
        }
    }

    @Transactional
    public void replayFailedEvent(Long id) {
        OutboxEvent event = outboxRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + id));
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);
        event.setNextRetryAt(null);
        event.setLastError(null);
        outboxRepo.save(event);
    }
}
