package com.gcp.repository;

import com.gcp.enums.Role;
import com.gcp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByCompanyId(Long companyId);

    List<User> findByRole(Role role);
}
