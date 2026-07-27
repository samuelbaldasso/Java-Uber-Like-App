package com.sbaldasso.combobackend.modules.notification.service;

import com.sbaldasso.combobackend.modules.delivery.domain.Delivery;
import com.sbaldasso.combobackend.modules.location.domain.Location;
import com.sbaldasso.combobackend.modules.location.repository.LocationRepository;
import com.sbaldasso.combobackend.modules.notification.domain.Notification;
import com.sbaldasso.combobackend.modules.notification.domain.NotificationType;
import com.sbaldasso.combobackend.modules.notification.repository.NotificationRepository;
import com.sbaldasso.combobackend.modules.user.domain.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final LocationRepository locationRepository;

  @Override
  @Async
  @Transactional
  public void notifyNearbyDrivers(Delivery delivery) {

    List<Location> nearbyDrivers = locationRepository.findByAvailableTrue();

    nearbyDrivers.forEach(location -> {
      Notification notification = new Notification();
      notification.setUser(location.getDriver());
      notification.setTitle("New Delivery Request");
      notification.setMessage("New delivery request from " + delivery.getPickupAddress());
      notification.setType(NotificationType.NEW_DELIVERY_REQUEST);
      notification.setData(delivery.getId().toString());

      notificationRepository.save(notification);
    });
  }

  @Override
  @Async
  @Transactional
  public void notifyDeliveryAccepted(Delivery delivery) {
    createNotification(
        delivery.getCustomer().getId(),
        "Driver Assigned",
        "A driver has accepted your delivery request",
        NotificationType.DELIVERY_ACCEPTED,
        delivery.getId().toString());
  }

  @Override
  @Async
  @Transactional
  public void notifyDeliveryStatusUpdate(Delivery delivery) {
    switch (delivery.getStatus()) {
          case PICKED_UP -> {
            createNotification(
                delivery.getCustomer().getId(),
                "Package Picked Up",
                "Your package has been picked up by the driver",
                NotificationType.DELIVERY_PICKED_UP,
                delivery.getId().toString());
          }
          case DELIVERED -> {
            createNotification(
                delivery.getCustomer().getId(),
                "Delivery Completed",
                "Your package has been delivered successfully",
                NotificationType.DELIVERY_COMPLETED,
                delivery.getId().toString());
          }
          case CANCELLED -> {
            createNotification(
                delivery.getCustomer().getId(),
                "Delivery Cancelled",
                "Your delivery has been cancelled",
                NotificationType.DELIVERY_CANCELLED,
                delivery.getId().toString());
          }
          default -> throw new IllegalArgumentException("Unexpected value: " + delivery.getStatus());
    }
  }

  @Override
  public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  @Override
  @Transactional
  public void markNotificationAsRead(UUID notificationId, UUID userId) {
    notificationRepository.findById(notificationId)
        .filter(notification -> notification.getUser().getId().equals(userId))
        .ifPresent(notification -> {
          notification.setRead(true);
          notification.setReadAt(LocalDateTime.now());
          notificationRepository.save(notification);
        });
  }

  @Override
  @Transactional
  public void deleteOldNotifications() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
    notificationRepository.deleteByCreatedAtBefore(cutoffDate);
  }

  private void createNotification(UUID userId, String title, String message,
      NotificationType type, String data) {
    Notification notification = new Notification();
    notification.setUser(new com.sbaldasso.combobackend.modules.user.domain.User(userId));
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setType(type);
    notification.setData(data);

    notificationRepository.save(notification);
  }
}