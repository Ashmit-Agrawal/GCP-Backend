package com.gcp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CompanyRegistrationRequest(
        @NotBlank(message = "Company name is required")
        String name,

        @NotBlank(message = "Sector is required")
        String sector,

        @NotBlank(message = "CIN is required")
        String cin,

        @NotBlank(message = "GSTIN is required")
        String gstin,

        @NotBlank(message = "PAN is required")
        String pan,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Contact person is required")
        String contactPerson,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotNull(message = "Annual turnover in ₹ Crores is required")
        @PositiveOrZero(message = "Annual turnover must be positive")
        Double annualTurnoverCrores,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {}

