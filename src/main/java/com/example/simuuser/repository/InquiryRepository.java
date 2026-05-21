package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<Inquiry> findByIdAndUser(Long id, AppUser user);

    List<Inquiry> findAllByOrderByCreatedAtDesc();
}
