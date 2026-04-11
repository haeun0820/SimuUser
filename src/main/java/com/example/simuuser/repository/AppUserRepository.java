package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUserId(String userId);

    Optional<AppUser> findByProviderAndProviderId(String provider, String providerId);
}
