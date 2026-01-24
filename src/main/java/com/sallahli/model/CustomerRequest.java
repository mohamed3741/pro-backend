package com.sallahli.model;

import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerRequest extends HasTimestamps implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_request_id_seq")
    @SequenceGenerator(name = "customer_request_id_seq", sequenceName = "customer_request_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Location
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String addressText;

    private String landmark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    // Request details
    @Column(columnDefinition = "TEXT")
    private String descriptionText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_note_media_id")
    private Media voiceNoteMedia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.OPEN;

    @Builder.Default
    private Boolean urgent = true;

    // Timing
    private LocalDateTime broadcastedAt;
    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean archived = false;
}

