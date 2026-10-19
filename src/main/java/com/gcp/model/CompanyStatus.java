package com.gcp.model;

/**
 * Verification lifecycle for a company.
 * PENDING_VERIFICATION: Automated checks passed, awaiting officer approval.
 * APPROVED: Officer approved with assigned rank; company can log in and apply for parcels.
 * REJECTED: Officer rejected registration with reason.
 */
public enum CompanyStatus {
    PENDING_VERIFICATION,
    APPROVED,
    REJECTED
}

