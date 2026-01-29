package com.sallahli.mapper;

import com.sallahli.dto.chat.MessageDto;
import com.sallahli.model.Message;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@org.mapstruct.Mapper(componentModel = "spring", uses = { ConversationMapper.class })
public interface MessageMapper extends Mapper<Message, MessageDto> {

    @Override
    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderFirstName", ignore = true)
    @Mapping(target = "senderLastName", ignore = true)
    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderLogo", ignore = true)
    MessageDto toDto(Message model);

    @Named("toDtoWithoutConversation")

    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "senderFirstName", ignore = true)
    @Mapping(target = "senderLastName", ignore = true)
    @Mapping(target = "senderUsername", ignore = true)
    @Mapping(target = "senderLogo", ignore = true)
    MessageDto toDtoWithoutConversation(Message model);
}
