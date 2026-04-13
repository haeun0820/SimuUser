package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUserId(String userId);

    Optional<AppUser> findByUserId(String userId);

    Optional<AppUser> findFirstByEmail(String email);

    List<AppUser> findTop10ByEmailContainingIgnoreCase(String email);

    Optional<AppUser> findByProviderAndProviderId(String provider, String providerId);
}
