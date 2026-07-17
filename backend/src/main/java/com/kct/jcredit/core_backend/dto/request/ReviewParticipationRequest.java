package com.kct.jcredit.core_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.kct.jcredit.core_backend.dto.request.base.BaseRequest;

import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReviewParticipationRequest extends BaseRequest {
    @NotNull
    private String decision; // "APPROVED" / "REJECTED"

    private String rejectReason; //REJECTED

    @NotBlank
    private String reviewerSeal; // digital inkan

    @NotNull
    private Map<String, Boolean> docChecks;      // doc_check_1..7

    @NotNull
    private Map<String, Boolean> dataMgmtChecks; // data_mgmt_check_1..5
}
