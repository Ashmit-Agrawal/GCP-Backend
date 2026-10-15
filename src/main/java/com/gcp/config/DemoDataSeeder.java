package com.gcp.config;

import com.gcp.dto.CompanyRequest;
import com.gcp.dto.ParcelRequest;
import com.gcp.model.DegradationLevel;
import com.gcp.repository.LandParcelRepository;
import com.gcp.service.CompanyService;
import com.gcp.service.ParcelService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Loads a few sample parcels and one company so the frontend has something to show. */
@Component
@ConditionalOnProperty(name = "gcp.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final LandParcelRepository parcelRepo;
    private final ParcelService parcels;
    private final CompanyService companies;

    public DemoDataSeeder(LandParcelRepository parcelRepo, ParcelService parcels, CompanyService companies) {
        this.parcelRepo = parcelRepo;
        this.parcels = parcels;
        this.companies = companies;
    }

    @Override
    public void run(String... args) {
        if (parcelRepo.count() > 0) {
            return;
        }
        parcels.create(new ParcelRequest("Maharashtra", "MH", "Palghar Forest Belt",
                19.6967, 72.7699, 18.5, "Tropical Moist", DegradationLevel.MEDIUM));
        parcels.create(new ParcelRequest("West Bengal", "WB", "Sundarbans Fringe",
                21.9497, 88.9000, 24.0, "Mangrove", DegradationLevel.HIGH));
        parcels.create(new ParcelRequest("Karnataka", "KA", "Coorg Hills",
                12.4244, 75.7382, 9.0, "Tropical Moist", DegradationLevel.LOW));
        parcels.create(new ParcelRequest("Uttarakhand", "UK", "Doon Valley Slopes",
                30.3165, 78.0322, 14.0, "Temperate", DegradationLevel.MEDIUM));

        companies.register(new CompanyRequest("Demo Green Industries Pvt Ltd", "Manufacturing",
                "U12345MH2020PTC000001", "contact@demogreen.example", "9999999999"));
    }
}
