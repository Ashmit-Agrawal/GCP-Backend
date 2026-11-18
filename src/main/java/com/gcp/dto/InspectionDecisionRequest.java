package com.gcp.dto;

import jakarta.validation.constraints.NotBlank;

public record InspectionDecisionRequest(
        @NotBlank(message = "Inspection status is required") String inspectionStatus,
        String inspectorNotes,
        String officerName) {
}
