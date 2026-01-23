package com.sallahli.repository;

import com.sallahli.model.NotificationAttribute;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationAttributeRepository extends GenericRepository<NotificationAttribute> {

    List<NotificationAttribute> findByNotificationId(Long notificationId);

    List<NotificationAttribute> findByKeyAndNotificationId(String key, Long notificationId);
}

