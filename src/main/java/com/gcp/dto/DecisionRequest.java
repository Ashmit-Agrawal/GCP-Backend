package com.gcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Used by officers to approve or reject applications and redemption requests. */
public record DecisionRequest(
        @NotNull Boolean approve,
        @NotBlank String officerName,
        String reason) {
}
