package com.sallahli.dto;

import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.utils.HasTimestampsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

