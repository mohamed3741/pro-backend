package com.sallahli.repository;

import com.sallahli.model.Conversation;
import com.sallahli.model.Enum.ConversationType;
import com.sallahli.model.Enum.UserRoleEnum;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends GenericRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.role = :role AND p.userId = :userId AND c.type IN :types AND c.closed = :closed")
    Page<Conversation> findByTypeAndParticipant(
            @Param("types") List<ConversationType> types,
            @Param("role") UserRoleEnum role,
            @Param("userId") Long userId,
            @Param("closed") boolean closed,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.role = :role AND p.userId = :userId AND c.type IN :types AND c.closed = false")
    Page<Conversation> findOpenByTypeAndParticipant(
            @Param("types") List<ConversationType> types,
            @Param("role") UserRoleEnum role,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.customerRequest.id = :customerRequestId AND c.type = :type AND c.closed = false")
    Conversation findOpenConversationByOrderAndType(
            @Param("customerRequestId") Long customerRequestId,
            @Param("type") ConversationType type);

    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
           "WHERE p1.role = :role1 AND p1.userId = :userId1 AND p2.role = :role2 AND p2.userId = :userId2 " +
           "AND c.type IN :types AND c.closed = false")
    Conversation findOpenConversationBetweenUserAndEmployee(
            @Param("types") List<ConversationType> types,
            @Param("role1") UserRoleEnum role1,
            @Param("userId1") Long userId1,
            @Param("role2") UserRoleEnum role2,
            @Param("userId2") Long userId2);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId AND p.role = :role")
    Page<Conversation> findConversationsForUser(
            @Param("userId") Long userId,
            @Param("role") UserRoleEnum role,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participants p JOIN c.messages m " +
           "WHERE p.userId = :userId AND p.role = :role " +
           "AND m.senderRole IN :senderRoles AND m.isRead = false")
    Page<Conversation> findUnreadConversationsForUser(
            @Param("userId") Long userId,
            @Param("role") UserRoleEnum role,
            @Param("senderRoles") List<UserRoleEnum> senderRoles,
            Pageable pageable);

    @Query("SELECT COUNT(c) FROM Conversation c JOIN c.participants p " +
           "WHERE p.role IN :employeeRoles AND c.id NOT IN " +
           "(SELECT c2.id FROM Conversation c2 JOIN c2.participants p2 WHERE p2.role IN :employeeRoles)")
    Long countConversationsWithoutAdminParticipants(
            @Param("employeeRoles") List<UserRoleEnum> employeeRoles);

    @Query("SELECT COUNT(m) FROM Message m JOIN m.conversation c JOIN c.participants p " +
           "WHERE p.role IN :roles AND m.senderRole IN :senderRoles AND m.isRead = false")
    Long countUnreadMessagesFromParticipantByType(
            @Param("types") List<ConversationType> types,
            @Param("senderRoles") List<UserRoleEnum> senderRoles);

    @Query("SELECT COUNT(m) FROM Message m JOIN m.conversation c JOIN c.participants p " +
           "WHERE c.type IN :types AND p.userId = :userId AND p.role = :role " +
           "AND m.senderRole IN :senderRoles AND m.isRead = false")
    Long countUnreadMessagesForEmployee(
            @Param("types") List<ConversationType> types,
            @Param("userId") Long userId,
            @Param("role") UserRoleEnum role,
            @Param("senderRoles") List<UserRoleEnum> senderRoles);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.senderId != :userId AND m.isRead = false")
    Long countUnreadMessagesForParticipantInConversation(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m JOIN m.conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND m.senderRole IN :senderRoles AND m.isRead = false")
    Long countUnreadMessagesForUser(
            @Param("userId") Long userId,
            @Param("senderRoles") List<UserRoleEnum> senderRoles);
}
