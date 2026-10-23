package com.gcp.service;

import com.gcp.dto.CompanyVerificationResult;
import com.gcp.enums.CompanyRank;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Handles automated verification of company statutory identifiers (GSTIN, PAN,
 * CIN)
 * and calculates suggested company ranking and parcel registration limits.
 */
@Service
public class CompanyVerificationService {

    // 15 alphanumeric characters: 2 state code + 5 chars + 4 digits + 1 char + 1
    // entity + Z + 1 checksum
    private static final Pattern GSTIN_PATTERN = Pattern
            .compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    // 10 alphanumeric characters: 5 chars + 4 digits + 1 char
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    // 21 alphanumeric characters: L/U + 5 digits + 2 state + 4 year + 3 type + 6
    // digits
    private static final Pattern CIN_PATTERN = Pattern.compile("^[LU][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$");

    public CompanyVerificationResult verify(String cin, String gstin, String pan, Double turnoverCrores) {
        String cleanCin = cin == null ? "" : cin.trim().toUpperCase();
        String cleanGstin = gstin == null ? "" : gstin.trim().toUpperCase();
        String cleanPan = pan == null ? "" : pan.trim().toUpperCase();

        boolean cinValid = CIN_PATTERN.matcher(cleanCin).matches();
        boolean gstinValid = GSTIN_PATTERN.matcher(cleanGstin).matches();
        boolean panValid = PAN_PATTERN.matcher(cleanPan).matches();

        // Automated consistency check: Characters 3-12 of GSTIN must match PAN
        boolean panMatchesGstin = false;
        if (gstinValid && panValid && cleanGstin.length() >= 12) {
            String gstinPanPart = cleanGstin.substring(2, 12);
            panMatchesGstin = gstinPanPart.equalsIgnoreCase(cleanPan);
        }

        boolean validOverall = cinValid && gstinValid && panValid && panMatchesGstin;

        CompanyRank suggestedRank = CompanyRank.determineFromTurnover(turnoverCrores);
        double maxArea = suggestedRank.getMaxAreaHectares();

        StringBuilder summary = new StringBuilder();
        if (validOverall) {
            summary.append(
                    "Automated checks PASSED: GSTIN, PAN, and CIN are syntactically valid and PAN matches GSTIN. ")
                    .append("Assigned Rank: ").append(suggestedRank.name())
                    .append(" (Max Land Limit: ").append(maxArea).append(" ha). Pending manual officer approval.");
        } else {
            summary.append("Automated checks flagged discrepancies: ");
            if (!cinValid)
                summary.append("[Invalid CIN format] ");
            if (!gstinValid)
                summary.append("[Invalid GSTIN format] ");
            if (!panValid)
                summary.append("[Invalid PAN format] ");
            if (gstinValid && panValid && !panMatchesGstin) {
                summary.append("[PAN does not match characters 3-12 of GSTIN] ");
            }
        }

        return new CompanyVerificationResult(
                gstinValid,
                panValid,
                cinValid,
                panMatchesGstin,
                validOverall,
                suggestedRank,
                maxArea,
                summary.toString());
    }
}
