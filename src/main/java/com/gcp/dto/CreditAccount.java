package com.gcp.dto;

/** The numbers shown on the company's Credits Dashboard. */
public record CreditAccount(
        Long companyId,
        double totalEarned,
        double redeemed,
        double available,
        double inPipeline) {
}
