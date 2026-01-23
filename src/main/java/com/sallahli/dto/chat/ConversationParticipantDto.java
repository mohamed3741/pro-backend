package com.sallahli.dto.chat;

import com.sallahli.model.Enum.UserRoleEnum;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationParticipantDto {

    private Long userId;
    private UserRoleEnum role;
    private String firstName;
    private String lastName;
    private String username;
    private String logo;
}
