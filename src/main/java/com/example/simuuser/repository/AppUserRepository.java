package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUserId(String userId);
}
