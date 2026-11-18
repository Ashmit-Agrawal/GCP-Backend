package com.gcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String parcelId,
        String idempotencyKey,
        String proposedSpecies,
        Integer saplingCount,
        String plantingPlanSummary,
        String supportingDocRef) {

    public ApplicationRequest(Long companyId, String parcelId) {
        this(companyId, parcelId, null, null, null, null, null);
    }

    public ApplicationRequest(Long companyId, String parcelId, String idempotencyKey) {
        this(companyId, parcelId, idempotencyKey, null, null, null, null);
    }
}
