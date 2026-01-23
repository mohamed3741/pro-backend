package com.sallahli.model;

import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import lombok.experimental.SuperBuilder;

import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, of = "id")
@Entity
@Table(name = "media")
public class Media extends HasTimestamps implements Archivable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_id_seq")
    @SequenceGenerator(name = "media_id_seq", sequenceName = "media_id_seq",  allocationSize=1)
    private Long id;
    @Enumerated(EnumType.STRING)
    private MediaEnum type;
    private String link;
    private String thumbnail;
    @Column(name = "key_name")
    private String keyName;
    @Column(name = "duration_millis")
    private Long durationMillis;
    @Column(name = "size_bytes")
    private Long sizeBytes;
    @Column(name = "mime_type")
    private String mimeType;

    @Builder.Default
    private Boolean archived = false;

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}



