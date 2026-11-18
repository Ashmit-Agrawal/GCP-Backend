package com.gcp.service;

import com.gcp.model.LandParcel;
import com.gcp.model.MonitoringRecord;

/**
 * Pluggable interface for Satellite Imagery & AI Canopy Analysis providers.
 * Initially backed by MockSatelliteMonitoringProvider; designed for drop-in replacement
 * by a real Google Earth Engine / ISRO Bhuvan / Sentinel-2 provider.
 */
public interface SatelliteMonitoringProvider {

    String getProviderName();

    String getModelVersion();

    MonitoringRecord analyzeParcel(LandParcel parcel, Double previousCanopyPercent);
}
