package com.gcp.dto;

import com.gcp.enums.RedemptionOption;
import jakarta.validation.constraints.NotNull;

public record RedemptionRequest(
                @NotNull Long companyId,
                @NotNull RedemptionOption option) {
}
