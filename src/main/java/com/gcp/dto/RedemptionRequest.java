package com.gcp.dto;

import com.gcp.enums.RedemptionOption;
import jakarta.validation.constraints.NotNull;

public record RedemptionRequest(
                @NotNull Long companyId,
                @NotNull RedemptionOption option,
                String idempotencyKey) {

        public RedemptionRequest(Long companyId, RedemptionOption option) {
                this(companyId, option, null);
        }
}
