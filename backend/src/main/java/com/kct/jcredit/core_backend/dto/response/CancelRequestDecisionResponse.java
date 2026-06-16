package com.kct.jcredit.core_backend.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

/**
 * 取消申請決定レスポンス
 * APPROVEとREJECTの両方に対応
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null フィールドは JSON に含めない
public class CancelRequestDecisionResponse extends BaseResponse {
    
    /**
     * 取消申請ID
     */
    private Integer cancelRequestId;
    
    /**
     * ステータス: APPROVED, REJECTED
     */
    private String status;
    
    /**
     * 承認/却下者
     */
    private String decidedBy;
    
    /**
     * 承認/却下日時
     */
    private String decidedAt;
    
    /**
     * 元のレコードID
     */
    private String originalRecordPk;
    
    /**
     * 元のレコードSK
     */
    private String originalRecordSk;
    
    /**
     * 相殺レコードID（承認時のみ）
     */
    private String offsetRecordPk;
    
    /**
     * 相殺レコードSK（承認時のみ）
     */
    private String offsetRecordSk;
    
    /**
     * 管理者コメント
     */
    private String managerRemarks;
}