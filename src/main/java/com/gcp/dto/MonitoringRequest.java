package com.gcp.dto;

import com.gcp.enums.RiskLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * One monitoring reading. For now entered by hand/seed; later filled by the AI
 * + satellite engine.
 */
public record MonitoringRequest(
                LocalDate recordedOn,
                @NotNull @DecimalMin("0") @DecimalMax("100") Double canopyPercent,
                Double biomassIndex,
                Double moistureStability,
                RiskLevel riskLevel,
                String note,
                String providerName,
                String modelVersion,
                Double confidenceScore,
                Boolean isSimulated,
                String inspectionStatus) {

        public MonitoringRequest(
                        LocalDate recordedOn,
                        Double canopyPercent,
                        Double biomassIndex,
                        Double moistureStability,
                        RiskLevel riskLevel,
                        String note) {
                this(recordedOn, canopyPercent, biomassIndex, moistureStability, riskLevel, note,
                                "MOCK_ISRO_SENTINEL_V1", "canopy-ai-sim-1.0", 0.88, true, "AI_ADVISORY_ONLY");
        }
}
