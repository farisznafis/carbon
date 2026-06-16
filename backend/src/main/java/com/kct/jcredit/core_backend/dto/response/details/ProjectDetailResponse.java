package com.kct.jcredit.core_backend.dto.response.details;

import java.time.Instant;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDetailResponse {

    private String id;

    private String projectName;
    private String projectNumber;
    private String projectStatus;      // 派生 or そのまま返却

    // 排出量）
    private String emissionReduction;
    private String baselineEmission;
    private String postProjectEmission;

    // 代表者
    private String repFirstName;
    private String repFirstNameKana;
    private String repLastName;
    private String repLastNameKana;
    private String repEmail;
    private String repPhoneNumber;
    private String repPosition;

    // 担当者
    private String picFirstName;
    private String picFirstNameKana;
    private String picLastName;
    private String picLastNameKana;
    private String picEmail;
    private String picPhoneNumber;
    private String picPosition;
    private String dataManagementPic;

    // 会社・地域
    private String companyName;
    private String regionalCategory;

    // 会員数
    private Integer newMembersTotal;
    private Integer withdrawnMembers;
    private Integer cumulativeMembers;
    private Integer membersTotal;

    // 方法論
    private String methodologyVersion;
    private String methodologyCategory;
    private String methodologyNumber;
    private String methodologyName;
    private String methodologyMainEmission;
    private Boolean methodCombiFeasibility;
    private String methodDetails;

    // 事業者会社ID
    private Integer operatorCompanyId;

    // 参加状況（派生）
    private Boolean isJoined;

    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}