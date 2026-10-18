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
    // 本社所在地 as the official form structures it. The frontend merges this
    // response straight into the application form values, so the names must
    // match ProjectApplicationRequest.
    private String city;
    private String address;
    private String representativePosition;
    private String representativeName;
    private String picPosition;
    private String picName;
    private String picEmail;
    private String picPhoneNumber;
}
