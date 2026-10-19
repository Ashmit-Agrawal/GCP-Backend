package com.gcp.dto;

public record AdminDashboardStats(
        long totalCompanies,
        long pendingCompanyVerifications,
        long pendingApplications,
        long pendingCreditApprovals,
        double totalCreditsIssued,
        double totalCreditsRedeemed,
        double totalLandAllocatedHectares,
        long totalAvailableParcels
) {}

