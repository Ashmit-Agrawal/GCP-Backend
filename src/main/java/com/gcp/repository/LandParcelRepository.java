package com.gcp.repository;

import com.gcp.enums.ParcelStatus;
import com.gcp.model.LandParcel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandParcelRepository extends MongoRepository<LandParcel, String> {
    long countByIdStartingWith(String prefix);

    List<LandParcel> findByAllottedCompanyId(Long allottedCompanyId);

    List<LandParcel> findByStatus(ParcelStatus status);
}
