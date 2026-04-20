package com.example.simuuser.repository;

import com.example.simuuser.entity.AppUser;
import com.example.simuuser.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientAndDeletedAtIsNullOrderByCreatedAtDesc(AppUser recipient);

    Optional<Notification> findByIdAndRecipient(Long id, AppUser recipient);

    Optional<Notification> findByRecipientAndTypeAndReferenceIdAndDeletedAtIsNull(AppUser recipient, String type, Long referenceId);
}
