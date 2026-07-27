package com.sbaldasso.combobackend.modules.notification.service;

import com.sbaldasso.combobackend.modules.delivery.domain.Delivery;
import com.sbaldasso.combobackend.modules.location.repository.LocationRepository;
import com.sbaldasso.combobackend.modules.notification.domain.Notification;
import com.sbaldasso.combobackend.modules.notification.repository.NotificationRepository;
import com.sbaldasso.combobackend.modules.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {
  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private LocationRepository locationRepository;
  @InjectMocks
  private NotificationServiceImpl notificationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getUserNotifications_returnsPage() {
    UUID userId = UUID.randomUUID();
    Page<Notification> page = new PageImpl<>(Collections.emptyList());
    when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())).thenReturn(page);
    Page<Notification> result = notificationService.getUserNotifications(userId, Pageable.unpaged());
    assertEquals(page, result);
  }

  @Test
  void markNotificationAsRead_updatesNotification() {
    UUID notificationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Notification notification = new Notification();
    notification.setUser(new User(userId));
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
    notificationService.markNotificationAsRead(notificationId, userId);
    assertTrue(notification.isRead());
    assertNotNull(notification.getReadAt());
    verify(notificationRepository).save(notification);
  }

  @Test
  void deleteOldNotifications_deletesByDate() {
    notificationService.deleteOldNotifications();
    verify(notificationRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
  }
}
