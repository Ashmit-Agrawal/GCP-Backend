package com.gcp.dto;

import com.gcp.enums.CompanyRank;
import jakarta.validation.constraints.NotNull;

public record RankUpdateRequest(
                @NotNull(message = "Company rank is required") CompanyRank rank,
                String officerName,
                String reason) {
}
