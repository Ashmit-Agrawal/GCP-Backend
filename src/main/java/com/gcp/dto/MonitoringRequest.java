package com.gcp.dto;

import com.gcp.model.RiskLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** One monitoring reading. For now entered by hand/seed; later filled by the AI + satellite engine. */
public record MonitoringRequest(
        LocalDate recordedOn,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double canopyPercent,
        Double biomassIndex,
        Double moistureStability,
        RiskLevel riskLevel,
        String note) {
}
