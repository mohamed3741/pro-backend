package com.sallahli.model;

import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "translation_values")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TranslationValues extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "translation_seq_id")
    @SequenceGenerator(name = "translation_seq_id", sequenceName = "translation_seq_id", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String languageCode;

    @Column(nullable = false)
    private String translationKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translationValue;

    private String context;

    @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"language_code", "translation_key"}))
    public static class UniqueLanguageKeyConstraint {}
}

