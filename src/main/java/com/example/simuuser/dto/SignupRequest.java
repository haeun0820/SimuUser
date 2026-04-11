package com.example.simuuser.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class SignupRequest {

    private String name;
    private String userId;
    private String password;
    private String passwordConfirm;
    private String email;
    private String phone;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;
    private String gender;
    private boolean privacyAgree;
    private boolean termsAgree;
    private boolean allAgree;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isPrivacyAgree() {
        return privacyAgree;
    }

    public void setPrivacyAgree(boolean privacyAgree) {
        this.privacyAgree = privacyAgree;
    }

    public boolean isTermsAgree() {
        return termsAgree;
    }

    public void setTermsAgree(boolean termsAgree) {
        this.termsAgree = termsAgree;
    }

    public boolean isAllAgree() {
        return allAgree;
    }

    public void setAllAgree(boolean allAgree) {
        this.allAgree = allAgree;
    }
}
