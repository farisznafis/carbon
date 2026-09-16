package com.kct.jcredit.core_backend.dto.response.details;



import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FileErrorDetailsResponse {
    private Integer maxSizeMB;
    private Double actualSizeMB;
    private List<String> allowedTypes;
    private String receivedType;
}