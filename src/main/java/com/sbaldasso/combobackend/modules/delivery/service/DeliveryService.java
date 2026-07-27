package com.sbaldasso.combobackend.modules.delivery.service;

import com.sbaldasso.combobackend.modules.delivery.domain.Delivery;
import com.sbaldasso.combobackend.modules.delivery.domain.DeliveryStatus;
import com.sbaldasso.combobackend.modules.delivery.dto.CreateDeliveryRequest;
import com.sbaldasso.combobackend.modules.delivery.dto.DeliveryResponse;
import com.sbaldasso.combobackend.modules.delivery.repository.DeliveryRepository;
import com.sbaldasso.combobackend.modules.location.service.LocationService;
import com.sbaldasso.combobackend.modules.notification.service.NotificationService;
import com.sbaldasso.combobackend.modules.user.domain.User;
import com.sbaldasso.combobackend.modules.user.domain.UserType;
import com.sbaldasso.combobackend.modules.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

  private final DeliveryRepository deliveryRepository;
  private final UserService userService;
  private final LocationService locationService;
  private final NotificationService notificationService;

  @Transactional
  public DeliveryResponse createDelivery(UUID customerId, CreateDeliveryRequest request) {
    User customer = userService.validateAndGetUser(customerId, UserType.CUSTOMER);

    Delivery delivery = new Delivery();
    delivery.setCustomer(customer);
    delivery.setPickupAddress(request.getPickupAddress());
    delivery.setDeliveryAddress(request.getDeliveryAddress());
    delivery.setPrice(request.getPrice());
    delivery.setDescription(request.getDescription());
    delivery.setStatus(DeliveryStatus.PENDING);

    delivery = deliveryRepository.save(delivery);
    notificationService.notifyNearbyDrivers(delivery);

    return toDeliveryResponse(delivery);
  }

  @Transactional
  public DeliveryResponse acceptDelivery(UUID deliveryId, UUID driverId) {
    User driver = userService.validateAndGetUser(driverId, UserType.DRIVER);
    Delivery delivery = getDeliveryById(deliveryId);

    if (delivery.getStatus() != DeliveryStatus.PENDING) {
      throw new IllegalStateException("Delivery cannot be accepted - current status: " + delivery.getStatus());
    }

    delivery.setDriver(driver);
    delivery.setStatus(DeliveryStatus.DRIVER_ASSIGNED);
    delivery = deliveryRepository.save(delivery);

    notificationService.notifyDeliveryAccepted(delivery);

    return toDeliveryResponse(delivery);
  }

  @Transactional
  public DeliveryResponse updateDeliveryStatus(UUID deliveryId, UUID driverId, DeliveryStatus newStatus) {
    User driver = userService.validateAndGetUser(driverId, UserType.DRIVER);
    Delivery delivery = getDeliveryById(deliveryId);

    if (!driver.getId().equals(delivery.getDriver().getId())) {
      throw new IllegalStateException("Driver is not assigned to this delivery");
    }

    validateStatusTransition(delivery.getStatus(), newStatus);

    delivery.setStatus(newStatus);

    switch (newStatus) {
      case PICKED_UP -> delivery.setPickedUpAt(LocalDateTime.now());
      case DELIVERED -> delivery.setDeliveredAt(LocalDateTime.now());
      default -> {
      }
    }

    delivery = deliveryRepository.save(delivery);
    notificationService.notifyDeliveryStatusUpdate(delivery);

    return toDeliveryResponse(delivery);
  }

  public Page<DeliveryResponse> getDeliveriesForUser(UUID userId, Pageable pageable) {
    User user = userService.validateAndGetUser(userId);
    Page<Delivery> deliveries;

    if (user.getUserType() == UserType.DRIVER) {
      deliveries = deliveryRepository.findByDriverId(userId, pageable);
    } else if (user.getUserType() == UserType.CUSTOMER) {
      deliveries = deliveryRepository.findByCustomerId(userId, pageable);
    } else {
      deliveries = deliveryRepository.findAll(pageable);
    }

    return deliveries.map(this::toDeliveryResponse);
  }

  public DeliveryResponse getDeliveryResponseById(UUID deliveryId) {
    return toDeliveryResponse(getDeliveryById(deliveryId));
  }

  private Delivery getDeliveryById(UUID deliveryId) {
    return deliveryRepository.findById(deliveryId)
        .orElseThrow(() -> new EntityNotFoundException("Delivery not found with id: " + deliveryId));
  }

  private void validateStatusTransition(DeliveryStatus currentStatus, DeliveryStatus newStatus) {
    boolean isValid = switch (currentStatus) {
      case PENDING -> newStatus == DeliveryStatus.DRIVER_ASSIGNED || newStatus == DeliveryStatus.CANCELLED;
      case DRIVER_ASSIGNED -> newStatus == DeliveryStatus.PICKED_UP || newStatus == DeliveryStatus.CANCELLED;
      case PICKED_UP -> newStatus == DeliveryStatus.IN_TRANSIT;
      case IN_TRANSIT -> newStatus == DeliveryStatus.DELIVERED || newStatus == DeliveryStatus.CANCELLED;
      case DELIVERED, CANCELLED -> false;
    };

    if (!isValid) {
      throw new IllegalStateException(
          "Invalid status transition from " + currentStatus + " to " + newStatus);
    }
  }

  private DeliveryResponse toDeliveryResponse(Delivery delivery) {
    DeliveryResponse response = new DeliveryResponse();
    response.setId(delivery.getId());
    response.setCustomerId(delivery.getCustomer().getId());
    response.setDriverId(delivery.getDriver() != null ? delivery.getDriver().getId() : null);
    response.setPickupAddress(delivery.getPickupAddress());
    response.setDeliveryAddress(delivery.getDeliveryAddress());
    response.setPrice(delivery.getPrice());
    response.setStatus(delivery.getStatus());
    response.setDescription(delivery.getDescription());
    response.setCreatedAt(delivery.getCreatedAt());
    response.setUpdatedAt(delivery.getUpdatedAt());
    response.setPickedUpAt(delivery.getPickedUpAt());
    response.setDeliveredAt(delivery.getDeliveredAt());
    response.setEstimatedDeliveryTime(delivery.getEstimatedDeliveryTime());

    if (delivery.getDriver() != null) {
      locationService.getDriverLocation(delivery.getDriver().getId()).ifPresent(location -> {
        response.setDriverLatitude(location.getLatitude());
        response.setDriverLongitude(location.getLongitude());
      });
    }

    return response;
  }
}