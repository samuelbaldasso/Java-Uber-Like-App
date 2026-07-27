package com.sbaldasso.combobackend.modules.notification.controller;

import com.sbaldasso.combobackend.modules.notification.domain.Notification;
import com.sbaldasso.combobackend.modules.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {
  @Mock
  private NotificationService notificationService;
  @InjectMocks
  private NotificationController notificationController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getUserNotifications_returnsPage() {
    UUID userId = UUID.randomUUID();
    Page<Notification> page = new PageImpl<>(Collections.emptyList());
    when(notificationService.getUserNotifications(userId, Pageable.unpaged())).thenReturn(page);
    ResponseEntity<Page<Notification>> result = notificationController.getUserNotifications(userId, Pageable.unpaged());
    assertEquals(page, result.getBody());
  }

  @Test
  void markAsRead_returnsOk() {
    UUID notificationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ResponseEntity<Void> result = notificationController.markAsRead(notificationId, userId);
    assertEquals(200, result.getStatusCodeValue());
    verify(notificationService).markNotificationAsRead(notificationId, userId);
  }

  @Test
  void cleanupOldNotifications_returnsOk() {
    ResponseEntity<Void> result = notificationController.cleanupOldNotifications();
    assertEquals(200, result.getStatusCodeValue());
    verify(notificationService).deleteOldNotifications();
  }
}
