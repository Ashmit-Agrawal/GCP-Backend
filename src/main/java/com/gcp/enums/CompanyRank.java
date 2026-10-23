package com.gcp.enums;

/**
 * Company Rank based on business scale/turnover.
 * Enforces maximum area (in hectares) for land parcel registration.
 */
public enum CompanyRank {
    PLATINUM(50.0, 500.0, "Tier 1: Large Enterprise (> ₹500 Cr) — Max 50.0 ha"),
    GOLD(25.0, 100.0, "Tier 2: Medium Enterprise (₹100 - ₹500 Cr) — Max 25.0 ha"),
    SILVER(15.0, 20.0, "Tier 3: Small Enterprise (₹20 - ₹100 Cr) — Max 15.0 ha"),
    BRONZE(5.0, 0.0, "Tier 4: Micro / Startup (< ₹20 Cr) — Max 5.0 ha");

    private final double maxAreaHectares;
    private final double minTurnoverCrores;
    private final String description;

    CompanyRank(double maxAreaHectares, double minTurnoverCrores, String description) {
        this.maxAreaHectares = maxAreaHectares;
        this.minTurnoverCrores = minTurnoverCrores;
        this.description = description;
    }

    public double getMaxAreaHectares() {
        return maxAreaHectares;
    }

    public double getMinTurnoverCrores() {
        return minTurnoverCrores;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines suggested rank from turnover in ₹ Crores.
     */
    public static CompanyRank determineFromTurnover(Double turnoverCrores) {
        if (turnoverCrores == null || turnoverCrores < 20.0) {
            return BRONZE;
        } else if (turnoverCrores < 100.0) {
            return SILVER;
        } else if (turnoverCrores < 500.0) {
            return GOLD;
        } else {
            return PLATINUM;
        }
    }
}
