package com.gcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreditDecisionRequest(
        @NotNull Boolean approve,
        @NotBlank String officerName,
        @PositiveOrZero Double revisedCredits,
        String remarks) {
}
