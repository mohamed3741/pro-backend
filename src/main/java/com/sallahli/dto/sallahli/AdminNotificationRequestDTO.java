package com.sallahli.dto.sallahli;

import com.sallahli.model.Enum.NotificationType;
import com.sallahli.model.Enum.TargetAudience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationRequestDTO {

    private String title;
    private String content;
    private NotificationType type;

    private TargetAudience targetAudience;

    // IDs of pros or clients if TargetAudience is SPECIFIC_PROS or SPECIFIC_CLIENTS
    private List<Long> targetIds;

    private Long businessId;
    private String servedApp;
}
