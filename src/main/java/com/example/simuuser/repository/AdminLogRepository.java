package com.example.simuuser.repository;

import com.example.simuuser.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    List<AdminLog> findAllByOrderByCreatedAtDesc();
}
