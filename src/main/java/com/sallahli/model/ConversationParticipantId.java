package com.sallahli.model;

import com.sallahli.model.Enum.UserRoleEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConversationParticipantId implements Serializable {
    @Serial
    private static final long serialVersionUID = 740764639631065563L;

    private Long conversation;
    private Long userId;
    private UserRoleEnum role;
}
