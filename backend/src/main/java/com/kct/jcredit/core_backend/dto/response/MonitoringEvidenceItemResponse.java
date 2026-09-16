package com.kct.jcredit.core_backend.dto.response;

 
import java.util.List;
 
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.enums.EvidenceRequirement;
import com.kct.jcredit.core_backend.enums.EvidenceType;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
/**
 * 証憑項目1件分の判定結果。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MonitoringEvidenceItemResponse {
 
    /** 入力項目名（例: elPv） */
    private String fieldName;
 
    /** 画面表示用の名称 */
    private String labelJa;
 
    /** 必須区分 */
    private EvidenceRequirement requirement;
 
    /** NOT_APPLICABLE の場合の理由。それ以外は null */
    private String notApplicableReason;
 
    /** OPTIONAL の場合、未提出時の取り扱い。それ以外は null */
    private String fallbackNote;
 
    /** 受け付ける証憑の種類。NOT_APPLICABLE の場合は空リスト */
    private List<EvidenceType> acceptedTypes;

    /** 提出状況: SUBMITTED / NOT_SUBMITTED。対象外の場合は null */
    private String status;

    /** 提出済みの場合のS3キー。未提出または対象外の場合は null */
    private String evidenceUrl;
}