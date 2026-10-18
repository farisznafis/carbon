package com.kct.jcredit.core_backend.dto.response.details;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDetailResponse {

    private Integer id;
    private String title;
    private String message;
    private Boolean read;
    private Integer operatingUserId;
    private Integer participatingUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
}
