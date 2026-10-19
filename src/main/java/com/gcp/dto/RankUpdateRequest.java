package com.gcp.dto;

import com.gcp.model.CompanyRank;
import jakarta.validation.constraints.NotNull;

public record RankUpdateRequest(
        @NotNull(message = "Company rank is required")
        CompanyRank rank,
        String officerName,
        String reason
) {}

