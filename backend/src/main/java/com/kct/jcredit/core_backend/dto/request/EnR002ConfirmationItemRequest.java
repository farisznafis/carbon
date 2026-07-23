package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.kct.jcredit.core_backend.dto.request.base.BaseRequest;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnR002ConfirmationItemRequest extends BaseRequest {
 
   // ── Consent ─────────────────────────
    @NotNull
    private Boolean isAgreeDataUse;

    @NotNull
    private Boolean isAgreeExtraData;

    @NotNull
    private Boolean isAgreeTransfer;

    @NotNull
    private Boolean isNotRegistered;

    // 参加しています / 参加していません 
    @NotNull
    private Boolean isKeidanrenParticipation;

    // ── 特定排出者 (conditional) ──────────────────────────────────
    @NotNull
    private Boolean isEmitter;

    // conditional
    private String emitterCode;

    private Boolean isAdjustedEmissionsAdded;

    // ── 省エネ法 (conditional) ────────────────────────────────────
    @NotNull
    private Boolean isSubjectToPeriodicReport;

    // conditional:isSubjectToPeriodicReport = true
    // true = 一種, false = 二種
    private Boolean isPeriodicReportTypeOne;

    // 7桁の数字
    private String energyBusinessCode;

    // 7桁の数字
    private String factoryCode;
}