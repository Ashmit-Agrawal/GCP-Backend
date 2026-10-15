package com.gcp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name,
        String sector,
        @NotBlank String cin,
        @Email String email,
        String phone) {
}
