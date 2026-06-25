package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorporationMasterData {
    private String corpNumber;
    private String corpName;
    private String corpPosition;
    private String prefecture;
    private String municipality;
    private String blocknum;
    private String buildingName;
    private String representativePosition;
    private String representativeName;
    private String picPosition;
    private String picName;
    private String picEmail;
    private String picPhoneNumber;
}
