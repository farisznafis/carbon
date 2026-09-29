package com.kct.jcredit.core_backend.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
/**
 * Wrapper response for equipment list retrieval, following the same
 * statusCode/message convention as SiteListResponse.
 * SiteListResponseと同じstatusCode/message規約に従う設備一覧用ラッパーレスポンス。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class EnR002EquipmentListResponse extends BaseResponse {
    private List<EnR002EquipmentResponse> equipments;
}
