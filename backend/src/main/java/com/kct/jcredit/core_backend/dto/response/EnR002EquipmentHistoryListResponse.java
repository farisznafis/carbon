package com.kct.jcredit.core_backend.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryResponse;

/**
 * Wrapper response for equipment change-history retrieval, following the
 * same statusCode/message convention as SiteListResponse.
 * SiteListResponseと同じstatusCode/message規約に従う設備変更履歴用ラッパーレスポンス。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnR002EquipmentHistoryListResponse extends BaseResponse {

    private List<EnR002EquipmentHistoryResponse> history;
}
