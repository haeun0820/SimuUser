package com.example.simuuser.controller;

import com.example.simuuser.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ResponseBody
    @GetMapping("/api/notifications")
    public ResponseEntity<?> notifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.findMine(authentication));
    }

    @ResponseBody
    @DeleteMapping("/api/notifications/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.delete(notificationId, authentication);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/api/notifications")
    public ResponseEntity<?> deleteAllNotifications(Authentication authentication) {
        notificationService.deleteAll(authentication);
        return ResponseEntity.ok().build();
    }
}
