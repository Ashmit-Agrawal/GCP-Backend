package com.gcp.dto;

import com.gcp.model.Company;

public record AdminCompanyDetails(
        Company company,
        CompanyVerificationResult verificationReport
) {}

