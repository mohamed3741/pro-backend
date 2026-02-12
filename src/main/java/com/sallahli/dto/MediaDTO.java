package com.sallahli.dto;

import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.utils.HasTimestampsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class MediaDTO extends HasTimestampsDTO {
    private Long id;
    private MediaEnum type;
    private String link;
    private String thumbnail;
    private String keyName;
    private Long durationMillis;
    private Long sizeBytes;
    private String mimeType;
}
