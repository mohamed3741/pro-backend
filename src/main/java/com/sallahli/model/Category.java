package com.sallahli.model;

import com.sallahli.model.Enum.LeadType;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Category extends HasTimestamps implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_id_seq")
    @SequenceGenerator(name = "category_id_seq", sequenceName = "category_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // PLUMBING, ELECTRICITY, HVAC

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_media_id")
    private Media iconMedia;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_type", nullable = false)
    @Builder.Default
    private LeadType leadType = LeadType.FIXED;

    @Column(name = "lead_cost", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal leadCost = BigDecimal.valueOf(50);

    @Column(name = "match_limit", nullable = false)
    @Builder.Default
    private Integer matchLimit = 3;

    /**
     * Workflow strategy for lead distribution:
     * - LEAD_OFFER: Traditional auction model (leads offered to multiple pros)
     * - FIRST_CLICK: Uber-style model (first pro to accept wins)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_type", nullable = false)
    @Builder.Default
    private WorkflowType workflowType = WorkflowType.LEAD_OFFER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean archived = false;
}
