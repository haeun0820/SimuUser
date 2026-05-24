package com.example.simuuser.config;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class AdminAccountInitializer {

    @Bean
    public ApplicationRunner adminAccountRunner(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.user-id:admin}") String adminUserId,
            @Value("${app.admin.password:admin1234!}") String adminPassword,
            @Value("${app.admin.name:관리자}") String adminName,
            @Value("${app.admin.email:admin@simuuser.local}") String adminEmail,
            @Value("${app.admin.phone:010-0000-0000}") String adminPhone,
            @Value("${app.admin.birth-date:1990-01-01}") String adminBirthDate,
            @Value("${app.admin.gender:male}") String adminGender
    ) {
        return args -> {
            AppUser adminUser = appUserRepository.findByUserId(adminUserId)
                    .orElseGet(() -> new AppUser(
                            adminName,
                            adminUserId,
                            passwordEncoder.encode(adminPassword),
                            adminEmail,
                            adminPhone,
                            LocalDate.parse(adminBirthDate),
                            adminGender
                    ));

            adminUser.updateProfile(adminName, adminPhone, LocalDate.parse(adminBirthDate), adminGender);
            adminUser.setEmail(adminEmail);
            adminUser.changePassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole("ADMIN");
            adminUser.setProfileCompleted(true);

            appUserRepository.save(adminUser);
        };
    }
}
