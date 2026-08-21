package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import java.util.List;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
/**
 * 基本設計 7.2.1「証憑項目表示」のレスポンス。
 *
 * 入力されたモニタリングデータに対して、証憑が必要な入力項目を明確に返却する。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MonitoringEvidenceItemsResponse extends BaseResponse {
    private Integer equipmentId;
    private String methodologyName;
    private String methodologyNumber;
    private String methodologyVersion;
    private List<MonitoringEvidenceItemResponse> evidenceItems;
}
 
