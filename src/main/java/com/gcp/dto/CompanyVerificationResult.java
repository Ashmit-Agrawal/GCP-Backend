package com.gcp.dto;

import com.gcp.enums.CompanyRank;

public record CompanyVerificationResult(
                boolean gstinValid,
                boolean panValid,
                boolean cinValid,
                boolean panMatchesGstin,
                boolean validOverall,
                CompanyRank suggestedRank,
                double maxAreaAllowedHa,
                String summaryMessage) {
}
