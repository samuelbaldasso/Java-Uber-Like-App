package com.sbaldasso.combobackend.modules.notification.service;

import com.sbaldasso.combobackend.modules.delivery.domain.Delivery;
import com.sbaldasso.combobackend.modules.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
  void notifyNearbyDrivers(Delivery delivery);

  void notifyDeliveryAccepted(Delivery delivery);

  void notifyDeliveryStatusUpdate(Delivery delivery);

  Page<Notification> getUserNotifications(UUID userId, Pageable pageable);

  void markNotificationAsRead(UUID notificationId, UUID userId);

  void deleteOldNotifications();
}