package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteResponse{
    private String siteId;
    private String siteName;  
    private String postalCode;
    private String prefecture;
    private String municipality;
    private String blocknum;
    private String buildingName;    
    private String picFirstName;
    private String picLastName;
    private List<String> projects;
    private SiteDataResponse data;
}
