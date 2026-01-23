package com.sallahli.repository;

import com.sallahli.model.Enum.NotificationType;
import com.sallahli.model.Notification;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends GenericRepository<Notification> {

    List<Notification> findByReadFalse();

    List<Notification> findByType(NotificationType type);

    List<Notification> findByBusinessId(Long businessId);

    @Query("SELECT n FROM Notification n WHERE n.read = false AND n.businessId = :businessId ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT n FROM Notification n WHERE n.channelName = :channelName AND n.channelToken = :channelToken")
    List<Notification> findByChannel(@Param("channelName") String channelName, @Param("channelToken") String channelToken);
}

