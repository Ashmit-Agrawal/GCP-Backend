package com.gcp.dto;

import com.gcp.enums.CompanyRank;
import jakarta.validation.constraints.NotBlank;

public record CompanyDecisionRequest(
                boolean approve,
                @NotBlank(message = "Officer name is required") String officerName,
                CompanyRank rank, // If null on approval, defaults to system-suggested rank
                String reason) {
}
