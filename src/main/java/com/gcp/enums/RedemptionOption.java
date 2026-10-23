package com.gcp.enums;

/** Redemption options and the credits (GC) each one costs. */
public enum RedemptionOption {
    CARBON_OFFSET_CERTIFICATE(100, "Offset verified corporate emissions"),
    ESG_REPORTING_CLAIM(150, "Annual sustainability declarations"),
    COMPLIANCE_ADJUSTMENT(200, "Environmental compliance metrics"),
    NET_ZERO_NEUTRALIZATION(250, "Long-term net-zero accounting");

    private final int cost;
    private final String purpose;

    RedemptionOption(int cost, String purpose) {
        this.cost = cost;
        this.purpose = purpose;
    }

    public int getCost() {
        return cost;
    }

    public String getPurpose() {
        return purpose;
    }
}
