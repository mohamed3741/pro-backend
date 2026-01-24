package com.sallahli.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorMessage {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  private int status;
  private String error;
  private String message;
  private String path;
  private List<FieldError> fieldErrors;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
  }

  public static ErrorMessage of(int status, String error, String message, String path) {
    return ErrorMessage.builder()
            .timestamp(LocalDateTime.now())
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .build();
  }
}

