package com.pro.repository;

import com.pro.model.NotificationAttribute;
import com.pro.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationAttributeRepository extends GenericRepository<NotificationAttribute> {

    List<NotificationAttribute> findByNotificationId(Long notificationId);

    List<NotificationAttribute> findByKeyAndNotificationId(String key, Long notificationId);
}
