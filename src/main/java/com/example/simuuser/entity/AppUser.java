package com.example.simuuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String email;

    @Column(length = 500)
    private String profileImage;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 20)
    private String gender;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 30)
    private String provider;

    @Column(length = 100)
    private String providerId;

    protected AppUser() {
    }

    public AppUser(String name, String userId, String password, String email, String phone, LocalDate birthDate, String gender) {
        this.name = name;
        this.userId = userId;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.provider = "LOCAL";
        this.providerId = null;
        this.createdAt = LocalDateTime.now();
    }

    public AppUser(String name, String userId, String password, String email, String phone, LocalDate birthDate, String gender, String provider, String providerId) {
        this.name = name;
        this.userId = userId;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void updateProfile(String name, String phone, LocalDate birthDate, String gender, String profileImage) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.profileImage = profileImage;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
