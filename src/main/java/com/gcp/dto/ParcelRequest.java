package com.gcp.dto;

import com.gcp.enums.DegradationLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParcelRequest(
                @NotBlank String state,
                @NotBlank String stateCode,
                String location,
                @NotNull Double latitude,
                @NotNull Double longitude,
                @NotNull @Positive Double areaHectares,
                String forestType,
                @NotNull DegradationLevel degradationLevel) {
}
