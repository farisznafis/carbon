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
public class EnR002FinalAgreementRequest { // TODO: aptchaToken 
    @NotNull
    private Boolean isAgreeTerms;
}