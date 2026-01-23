package com.sallahli.service;

import com.sallahli.dto.chat.*;
import com.sallahli.dto.search.PaginatedSearchService;
import com.sallahli.mapper.ConversationMapper;
import com.sallahli.mapper.ConversationParticipantsMapper;
import com.sallahli.mapper.MessageMapper;
import com.sallahli.model.*;
import com.sallahli.model.Enum.*;
import com.sallahli.repository.ConversationParticipantRepository;
import com.sallahli.repository.ConversationRepository;
import com.sallahli.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService implements PaginatedSearchService<ConversationDto, Conversation> {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final ConversationParticipantsMapper participantsMapper;
    private final MessageMapper messageMapper;
    private final ClientService clientService;
    private final ProService proService;

    private final List<UserRoleEnum> externalRoles = List.of(UserRoleEnum.CLIENT, UserRoleEnum.DRIVER, UserRoleEnum.PARTNER_MANAGER, UserRoleEnum.PARTNER_OWNER, UserRoleEnum.EMPLOYE);

    public ConversationDto getConversationById(long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return toEnrichedConversationDto(conv);
    }

    @Transactional
    public MessageDto sendMessage(Long conversationId, MessageDto messageDto) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Get connected user - simplified for now
        // TODO: Implement proper user authentication context
        Long senderId = getCurrentUserId(messageDto.getSenderRole());
        messageDto.setSenderId(senderId);

        Message msg = messageMapper.toModel(messageDto);
        msg.setConversation(conv);
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        messageDto = messageMapper.toDtoWithoutConversation(messageRepository.save(msg));
        // TODO: publishChatMessage(msg);
        return messageDto;
    }

    public Page<MessageDto> getMessages(Long conversationId, Pageable pageable) {
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        List<MessageDto> messagesDto = messages.get().toList().stream().map(this::toEnrichedMessageDto).toList();
        return new PageImpl<>(messagesDto, pageable, messages.getTotalElements());
    }

    @Transactional
    public void markConversationAsClosed(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conv.setClosed(true);
        conversationRepository.save(conv);
    }

    @Transactional
    public ConversationDto joinConversation(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        List<ConversationParticipant> currentParticipants = conv.getParticipants();
        if (currentParticipants == null) {
            currentParticipants = new ArrayList<>();
            conv.setParticipants(currentParticipants);
        }

        // TODO: Get current user properly
        Long userId = getCurrentUserId(UserRoleEnum.CUSTOMER_SUPPORT_AGENT);

        ConversationParticipant newParticipant = ConversationParticipant
                .builder()
                .userId(userId)
                .conversation(conv)
                .role(UserRoleEnum.CUSTOMER_SUPPORT_AGENT)
                .build();
        currentParticipants.add(newParticipant);

        return toEnrichedConversationDto(conversationRepository.save(conv));
    }

    public ConversationDto assign(Long conversationId, long employeeId) {
        Conversation conv = assignSupportParticipant(conversationId, employeeId);
        return toEnrichedConversationDto(conversationRepository.save(conv));
    }

    @Transactional
    public void markMessageAsRead(Long messageId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        messageRepository.markMessagesAsReadInConversation(msg.getConversation().getId());
    }

    @Transactional
    public ConversationDto initConversation(ConversationDto conversationDto, Long employeeId) {
        ConversationType type = conversationDto.getType();

        // Check for existing GENERAL_CLIENT_SUPPORT conversation
        if (type == ConversationType.GENERAL_CLIENT_SUPPORT) {
            // TODO: Get connected client
            Long clientId = getCurrentUserId(UserRoleEnum.CLIENT);

            List<Conversation> existing = conversationRepository.findOpenByTypeAndParticipant(
                    List.of(ConversationType.GENERAL_CLIENT_SUPPORT),
                    UserRoleEnum.CLIENT,
                    clientId,
                    PageRequest.of(0, 1)
            );
            if (!existing.isEmpty()) {
                Conversation existingConv = existing.get(0);
                if (employeeId != null) {
                    return assign(existingConv.getId(), employeeId);
                }
                return conversationMapper.toDto(existingConv);
            }
        }

        // Check for existing GENERAL_PRO_SUPPORT conversation
        if (type == ConversationType.GENERAL_PRO_SUPPORT) {
            // TODO: Get connected pro
            Long proId = getCurrentUserId(UserRoleEnum.DRIVER); // Using DRIVER role for Pro users

            var existing = conversationRepository.findOpenByTypeAndParticipant(
                    List.of(ConversationType.GENERAL_PRO_SUPPORT),
                    UserRoleEnum.DRIVER,
                    proId,
                    PageRequest.of(0, 1)
            );
            if (!existing.isEmpty()) {
                Conversation existingConv = existing.get(0);
                if (employeeId != null) {
                    return assign(existingConv.getId(), employeeId);
                }
                return conversationMapper.toDto(existingConv);
            }
        }

        // For general support conversations, check if one already exists
        if (type == ConversationType.GENERAL_CLIENT_SUPPORT) {
            // TODO: Get connected client
            Long clientId = getCurrentUserId(UserRoleEnum.CLIENT);

            List<Conversation> existing = conversationRepository.findOpenByTypeAndParticipant(
                    List.of(ConversationType.GENERAL_CLIENT_SUPPORT),
                    UserRoleEnum.CLIENT,
                    clientId,
                    PageRequest.of(0, 1)
            );
            if (!existing.isEmpty()) {
                Conversation existingConv = existing.get(0);
                if (employeeId != null) {
                    return assign(existingConv.getId(), employeeId);
                }
                return conversationMapper.toDto(existingConv);
            }
        }

        if (type == ConversationType.GENERAL_PRO_SUPPORT) {
            // TODO: Get connected pro
            Long proId = getCurrentUserId(UserRoleEnum.DRIVER); // Using DRIVER role for Pro users

            var existing = conversationRepository.findOpenByTypeAndParticipant(
                    List.of(ConversationType.GENERAL_PRO_SUPPORT),
                    UserRoleEnum.DRIVER,
                    proId,
                    PageRequest.of(0, 1)
            );
            if (!existing.isEmpty()) {
                Conversation existingConv = existing.get(0);
                if (employeeId != null) {
                    return assign(existingConv.getId(), employeeId);
                }
                return conversationMapper.toDto(existingConv);
            }
        }

        // Create and save conversation
        Conversation conv = conversationMapper.toModel(conversationDto);
        conv = conversationRepository.save(conv);

        // Initialize participants based on conversation type
        initParticipantsForType(conv);

        // Assign to employee if specified
        if (employeeId != null) {
            return assign(conv.getId(), employeeId);
        }

        return conversationMapper.toDto(conv);
    }

    private void initParticipantsForType(Conversation conv) {
        switch (conv.getType()) {
            case GENERAL_CLIENT_SUPPORT -> {
                Long clientId = getCurrentUserId(UserRoleEnum.CLIENT);
                createParticipant(conv, UserRoleEnum.CLIENT, clientId);
            }
            case GENERAL_PRO_SUPPORT -> {
                Long proId = getCurrentUserId(UserRoleEnum.DRIVER); // Using DRIVER role for Pro users
                createParticipant(conv, UserRoleEnum.DRIVER, proId);
            }
            case GENERAL_PARTNER_SUPPORT -> {
                // TODO: Handle partner support conversations
                throw new com.sallahli.exceptions.BadRequestException("Partner support conversations not implemented yet");
            }
            default -> {
                throw new com.sallahli.exceptions.BadRequestException("Conversation type not managed: " + conv.getType());
            }
        }
    }

    private void createParticipant(Conversation conv, UserRoleEnum role, Long userId) {
        participantRepository.save(
                ConversationParticipant.builder()
                        .conversation(conv)
                        .role(role)
                        .userId(userId)
                        .build()
        );
    }

    public Page<ConversationDto> findByParticipantAndType(ConversationByParticipantRequest request, Pageable pageable) {
        // TODO: Implement proper user authentication
        UserRoleEnum role = UserRoleEnum.CUSTOMER_SUPPORT_AGENT;
        Long userId = getCurrentUserId(role);

        Page<Conversation> conversations = conversationRepository
                .findByTypeAndParticipant(
                        request.getTypes(),
                        role,
                        userId,
                        request.isClosed(),
                        pageable);
        List<ConversationDto> content = conversations.stream().map(this::toEnrichedConversationDto).toList();

        return new PageImpl<>(content, pageable, conversations.getTotalElements());
    }

    @Transactional
    private Conversation assignSupportParticipant(Long conversationId, long employeeId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new com.sallahli.exceptions.NotFoundException("Conversation not found"));

        if (conv.getParticipants() == null) {
            conv.setParticipants(new ArrayList<>());
        }

        // Remove existing support agents
        conv.getParticipants().removeIf(p ->
                List.of(UserRoleEnum.ADMIN, UserRoleEnum.CUSTOMER_SUPPORT_AGENT).contains(p.getRole())
        );

        // Add new support agent
        ConversationParticipant supportParticipant = ConversationParticipant.builder()
                .userId(employeeId)
                .conversation(conv)
                .role(UserRoleEnum.CUSTOMER_SUPPORT_AGENT)
                .build();

        conv.getParticipants().add(supportParticipant);

        return conversationRepository.save(conv);
    }

    public Map<String, Long> getUnreadMessagesCount() {
        Map<String, Long> counts = new HashMap<>();

        counts.put("client", conversationRepository.countUnreadMessagesFromParticipantByType(
                List.of(ConversationType.GENERAL_CLIENT_SUPPORT),
                externalRoles
        ));

        counts.put("pro", conversationRepository.countUnreadMessagesFromParticipantByType(
                List.of(ConversationType.GENERAL_PRO_SUPPORT),
                externalRoles
        ));

        // TODO: Implement proper user authentication
        Long userId = getCurrentUserId(UserRoleEnum.CUSTOMER_SUPPORT_AGENT);

        counts.put("myMessages", conversationRepository.countUnreadMessagesForEmployee(
                List.of(
                        ConversationType.GENERAL_PARTNER_SUPPORT,
                        ConversationType.GENERAL_PRO_SUPPORT,
                        ConversationType.GENERAL_CLIENT_SUPPORT
                ),
                userId,
                UserRoleEnum.CUSTOMER_SUPPORT_AGENT,
                externalRoles));

        return counts;
    }

    public Long conversationsWithoutAdminParticipantsCount() {
        List<UserRoleEnum> employeeRoles = List.of(
                UserRoleEnum.ADMIN,
                UserRoleEnum.CUSTOMER_SUPPORT_AGENT,
                UserRoleEnum.ADMIN_PARTNER_MANAGER
        );

        return conversationRepository.countConversationsWithoutAdminParticipants(employeeRoles);
    }

    public Page<ConversationDto> getConversationsForConnectedUser(AppEnum app, Pageable pageable) {
        Long userId = getCurrentUserId(getUserRoleForApp(app));
        UserRoleEnum userRole = getUserRoleForApp(app);

        Page<Conversation> conversations = conversationRepository.findConversationsForUser(
                userId,
                userRole,
                pageable
        );

        List<ConversationDto> content = conversations.stream()
                .map(this::toEnrichedConversationDto)
                .toList();

        return new PageImpl<>(content, pageable, conversations.getTotalElements());
    }

    public Page<ConversationDto> getUnreadConversationsForConnectedUser(AppEnum app, Pageable pageable) {
        Long userId = getCurrentUserId(getUserRoleForApp(app));
        UserRoleEnum userRole = getUserRoleForApp(app);
        List<UserRoleEnum> senderRoles = getSenderRolesForApp(app);

        Page<Conversation> conversations = conversationRepository.findUnreadConversationsForUser(
                userId,
                userRole,
                senderRoles,
                pageable
        );

        List<ConversationDto> content = conversations.stream()
                .map(this::toEnrichedConversationDto)
                .toList();

        return new PageImpl<>(content, pageable, conversations.getTotalElements());
    }

    @Override
    public Page<Conversation> findBySpecification(Specification<Conversation> specification, Pageable pageable) {
        return conversationRepository.findAll(specification, pageable);
    }

    @Override
    public List<ConversationDto> mapData(List<Conversation> data) {
        return data.stream().map(this::toEnrichedConversationDto).toList();
    }

    private ConversationDto toEnrichedConversationDto(Conversation conversation) {
        ConversationDto dto = conversationMapper.toLightDto(conversation);

        Message lastMessage = messageRepository
                .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);

        dto.setLastMessage(toEnrichedMessageDto(lastMessage));

        if (conversation.getParticipants() != null) {
            dto.setParticipants(conversation.getParticipants().stream()
                    .map(this::toEnrichedParticipantDto)
                    .toList());
        }

        return dto;
    }

    private Long getCurrentUserId(UserRoleEnum role) {
        // TODO: Implement proper user authentication context
        // For now, return placeholder IDs
        return switch (role) {
            case CLIENT -> 1L;
            case DRIVER -> 2L;
            case ADMIN, CUSTOMER_SUPPORT_AGENT, ADMIN_PARTNER_MANAGER -> 3L;
            default -> 1L;
        };
    }

    private UserRoleEnum getUserRoleForApp(AppEnum app) {
        return switch (app) {
            case CLIENT -> UserRoleEnum.CLIENT;
            case DRIVER -> UserRoleEnum.DRIVER;
            default -> UserRoleEnum.CUSTOMER_SUPPORT_AGENT;
        };
    }

    private List<UserRoleEnum> getSenderRolesForApp(AppEnum app) {
        return switch (app) {
            case CLIENT -> List.of(UserRoleEnum.ADMIN, UserRoleEnum.CUSTOMER_SUPPORT_AGENT, UserRoleEnum.ADMIN_PARTNER_MANAGER);
            case DRIVER -> List.of(UserRoleEnum.ADMIN, UserRoleEnum.CUSTOMER_SUPPORT_AGENT, UserRoleEnum.ADMIN_PARTNER_MANAGER, UserRoleEnum.CLIENT);
            default -> externalRoles;
        };
    }

    private MessageDto toEnrichedMessageDto(Message message) {
        if (message == null) return null;

        MessageDto messageDto = messageMapper.toDtoWithoutConversation(message);

        // TODO: Enrich with sender information based on role
        switch (message.getSenderRole()) {
            case CLIENT -> {
                // TODO: Get client info
                messageDto.setSenderFirstName("Client");
                messageDto.setSenderLastName("Name");
                messageDto.setSenderUsername("client_username");
            }
            case DRIVER -> {
                // TODO: Get pro info (using DRIVER role for Pro users)
                messageDto.setSenderFirstName("Pro");
                messageDto.setSenderLastName("User");
                messageDto.setSenderUsername("pro_username");
            }
            default -> {
                // Admin/Support
                messageDto.setSenderFirstName("Support");
                messageDto.setSenderLastName("Agent");
                messageDto.setSenderUsername("support_username");
            }
        }

        return messageDto;
    }

    private ConversationParticipantDto toEnrichedParticipantDto(ConversationParticipant participant) {
        ConversationParticipantDto participantDto = participantsMapper.toDtoWithoutConversation(participant);

        // TODO: Enrich with user information based on role
        switch (participant.getRole()) {
            case CLIENT -> {
                participantDto.setFirstName("Client");
                participantDto.setLastName("Name");
                participantDto.setUsername("client_username");
            }
            case DRIVER -> {
                participantDto.setFirstName("Pro");
                participantDto.setLastName("User");
                participantDto.setUsername("pro_username");
            }
            default -> {
                participantDto.setFirstName("Support");
                participantDto.setLastName("Agent");
                participantDto.setUsername("support_username");
            }
        }

        return participantDto;
    }

    public Long getUnreadMessagesCountForConnectedUser(AppEnum app) {
        Long userId = getCurrentUserId(getUserRoleForApp(app));
        List<UserRoleEnum> senderRoles = getSenderRolesForApp(app);

        return conversationRepository.countUnreadMessagesForUser(userId, senderRoles);
    }
}
