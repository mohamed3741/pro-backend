package com.sallahli.mapper;

import com.sallahli.dto.chat.MessageDto;
import com.sallahli.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper extends Mapper<Message, MessageDto> {

    @Override
    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderFirstName", ignore = true)
    @Mapping(target = "senderLastName", ignore = true)
    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderLogo", ignore = true)
    MessageDto toDto(Message model);

    @Mapping(target = "conversation", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "senderFirstName", ignore = true)
    @Mapping(target = "senderLastName", ignore = true)
    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderLogo", ignore = true)
    MessageDto toDtoWithoutConversation(Message model);
}
