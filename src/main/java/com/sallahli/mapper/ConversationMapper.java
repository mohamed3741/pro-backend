package com.sallahli.mapper;

import com.sallahli.dto.chat.ConversationDto;
import com.sallahli.model.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ConversationParticipantsMapper.class, MessageMapper.class})
public interface ConversationMapper extends com.sallahli.mapper.Mapper<Conversation, ConversationDto> {

    @Override
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "messages", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    ConversationDto toDto(Conversation model);

    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "messages", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    ConversationDto toLightDto(Conversation model);
}
