package com.gcp.service;

import com.gcp.enums.DegradationLevel;
import com.gcp.enums.RiskLevel;
import com.gcp.model.LandParcel;
import com.gcp.model.MonitoringRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Deterministic Mock Satellite & AI Analysis Provider for local prototype and demonstration.
 * Explicitly tags all generated records with isSimulated = true and providerName = "MOCK_SENTINEL2_AI".
 */
@Component
public class MockSatelliteMonitoringProvider implements SatelliteMonitoringProvider {

    public static final String PROVIDER_NAME = "MOCK_SENTINEL2_AI";
    public static final String MODEL_VERSION = "canopy-ai-sim-1.0";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getModelVersion() {
        return MODEL_VERSION;
    }

    @Override
    public MonitoringRecord analyzeParcel(LandParcel parcel, Double previousCanopyPercent) {
        int hash = Math.abs(parcel.getId() != null ? parcel.getId().hashCode() : 42);

        double baseCanopy;
        if (previousCanopyPercent != null && previousCanopyPercent > 0) {
            double increment = 4.2 + (hash % 40) * 0.1; // +4.2% to +8.1%
            baseCanopy = Math.min(88.5, Math.round((previousCanopyPercent + increment) * 10.0) / 10.0);
        } else {
            double initial = parcel.getDegradationLevel() == DegradationLevel.HIGH ? 24.5
                    : parcel.getDegradationLevel() == DegradationLevel.MEDIUM ? 35.0 : 44.0;
            baseCanopy = Math.round((initial + (hash % 70) * 0.1) * 10.0) / 10.0;
        }

        double biomass = Math.round((0.85 + (baseCanopy / 65.0)) * 100.0) / 100.0;
        double moisture = Math.round((62.0 + (baseCanopy * 0.32)) * 10.0) / 10.0;
        double confidence = Math.round((0.84 + ((hash % 12) * 0.01)) * 100.0) / 100.0;

        RiskLevel risk = baseCanopy < 28.0 ? RiskLevel.HIGH
                : baseCanopy < 40.0 ? RiskLevel.MEDIUM : RiskLevel.LOW;

        String note = String.format(
                "[SIMULATED SATELLITE & AI PASS — %s] Multi-spectral NDVI canopy density measured at %.1f%% (Confidence: %.0f%%). %s",
                PROVIDER_NAME,
                baseCanopy,
                confidence * 100.0,
                risk == RiskLevel.LOW
                        ? "Strong sapling survival and canopy closure trajectory."
                        : "Moderate moisture stress detected; field officer verification recommended.");

        return MonitoringRecord.builder()
                .parcelId(parcel.getId())
                .recordedOn(LocalDate.now())
                .canopyPercent(baseCanopy)
                .biomassIndex(biomass)
                .moistureStability(moisture)
                .riskLevel(risk)
                .note(note)
                .providerName(PROVIDER_NAME)
                .modelVersion(MODEL_VERSION)
                .confidenceScore(confidence)
                .isSimulated(true)
                .inspectionStatus("AI_ADVISORY_ONLY")
                .build();
    }
}
