package com.gcp.dto;

import com.gcp.model.RedemptionOption;
import jakarta.validation.constraints.NotNull;

public record RedemptionRequest(
        @NotNull Long companyId,
        @NotNull RedemptionOption option) {
}
