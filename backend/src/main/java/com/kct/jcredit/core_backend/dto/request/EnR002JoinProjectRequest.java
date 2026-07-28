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
public class EnR002JoinProjectRequest {

    private EnR002EquipmentRequest equipment;

    private EnR002ConfirmationItemRequest confirmationItems;

    private EnR002SustainabilityCommitmentRequest sustainabilityCommitment;

    private EnR002FinalAgreementRequest finalAgreement;
}
