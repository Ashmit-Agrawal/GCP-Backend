package com.gcp.dto;

import com.gcp.model.VerificationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreditCalculationRequest(
        @NotBlank String parcelId,
        @NotNull @PositiveOrZero Double canopyGrowthPercent,
        @NotNull VerificationMethod verificationMethod) {
}
