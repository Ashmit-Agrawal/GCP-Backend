package com.gcp.dto;

public record PlantationActivityRequest(
        String plantationStatus,
        Integer saplingCount,
        String lastActivityNote,
        String proposedSpecies,
        String supportingDocRef) {
}
