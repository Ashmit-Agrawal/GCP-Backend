package com.gcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank String parcelId) {
}
