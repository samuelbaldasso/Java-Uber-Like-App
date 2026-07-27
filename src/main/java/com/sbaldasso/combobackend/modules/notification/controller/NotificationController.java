package com.sbaldasso.combobackend.modules.notification.controller;

import com.sbaldasso.combobackend.modules.notification.domain.Notification;
import com.sbaldasso.combobackend.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<Notification>> getUserNotifications(
      @RequestAttribute UUID userId,
      Pageable pageable) {
    return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
  }

  @PutMapping("/{notificationId}/read")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> markAsRead(
      @PathVariable UUID notificationId,
      @RequestAttribute UUID userId) {
    notificationService.markNotificationAsRead(notificationId, userId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/cleanup")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> cleanupOldNotifications() {
    notificationService.deleteOldNotifications();
    return ResponseEntity.ok().build();
  }
}