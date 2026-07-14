package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityComitmentRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;

public interface ProjectParticipationPdfService {
    // Orchestrator PDF — dipanggil dari afterCommit
    // return s3Key yang disimpan ke participation.s3_key_pdf_membership
    String createAndUpload(EnR002EquipmentRequest equipment,
                            EnR002ConfirmationItemRequest confirmationItems,
                            EnR002SustainabilityComitmentRequest sustainabilityCommitment,
                            ParticipatingProject participation);

    // Dipanggil dari download endpoint
    String generatePresignedUrl(String s3Key);
}
