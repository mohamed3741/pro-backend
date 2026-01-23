package com.pro.model;

import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lead_acceptance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadAcceptance extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lead_acceptance_id_seq")
    @SequenceGenerator(name = "lead_acceptance_id_seq", sequenceName = "lead_acceptance_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_offer_id", nullable = false, unique = true)
    private LeadOffer leadOffer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private CustomerRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_id", nullable = false)
    private Pro pro;

    @Column(nullable = false)
    private Long priceMru;

    private LocalDateTime acceptedAt;
}
