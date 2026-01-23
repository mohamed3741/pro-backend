package com.sallahli.mapper;

import com.sallahli.dto.chat.ConversationParticipantDto;
import com.sallahli.model.ConversationParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationParticipantsMapper extends Mapper<ConversationParticipant, ConversationParticipantDto> {

    @Override
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "logo", ignore = true)
    ConversationParticipantDto toDto(ConversationParticipant model);

    @Mapping(target = "conversation", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "logo", ignore = true)
    ConversationParticipantDto toDtoWithoutConversation(ConversationParticipant model);
}
