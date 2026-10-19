package com.gcp.dto;

import com.gcp.model.Company;
import com.gcp.model.Role;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        Role role,
        String fullName,
        Long companyId,
        Company company,
        OfficerInfo officer
) {
    public record OfficerInfo(
            String name,
            String role,
            String region,
            String employeeId,
            String ministry
    ) {}
}

