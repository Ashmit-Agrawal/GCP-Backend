package com.gcp.repository;

import com.gcp.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LandParcelRepository extends JpaRepository<LandParcel, String> {
    long countByIdStartingWith(String prefix);
}
