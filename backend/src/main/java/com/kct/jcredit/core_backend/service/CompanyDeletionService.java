package com.kct.jcredit.core_backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.DeleteCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.entity.CompanyDeletionRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.repository.postgres.CompanyDeletionRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.RecordCancelRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyDeletionService {

    private final ParticipatingCompanyRepository participatingCompanyRepository;
    private final SiteRepository siteRepository;
    private final ParticipatingUserRepository participatingUserRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final SiteR002MemberDataRepository siteR002MemberDataRepository;
    private final SiteR002EquipmentFileRepository siteR002EquipmentFileRepository;
    private final EnR002ReductionResultRepository reductionResultRepository;
    private final EnR002ReductionResultTotalRepository reductionResultTotalRepository;
    private final RecordCancelRequestRepository recordCancelRequestRepository;
    private final CompanyDeletionRequestRepository deletionRequestRepository;

    @Transactional
    public DeletionRequestResponse createDeletionRequest(String companyId, String requestedBy, String reason) {
        DeletionRequestResponse response = new DeletionRequestResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (company.getDeletedAt() != null) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_COMPANY_ALREADY_DELETED);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        if (deletionRequestRepository.existsByCompanyIdAndStatus(companyId, "PENDING")) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_DELETION_ALREADY_REQUESTED);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        try {
            CompanyDeletionRequest request = CompanyDeletionRequest.builder()
                    .companyId(companyId)
                    .requestedBy(requestedBy)
                    .requestedAt(LocalDateTime.now())
                    .reason(reason)
                    .status("PENDING")
                    .build();
            request = deletionRequestRepository.save(request);

            log.info("Deletion request created for company {} by {}", companyId, requestedBy);

            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.MSG_DELETION_REQUEST_SUBMITTED);
            response.setRequestId(request.getId());
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            response.setRequestedBy(requestedBy);
            response.setRequestedAt(request.getRequestedAt());
            response.setReason(reason);
            response.setStatus("PENDING");
        } catch (Exception e) {
            log.error("Failed to create deletion request for company {}: {}", companyId, e.getMessage(), e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        return response;
    }

    @Transactional
    public DeletionRequestResponse approveDeletionRequest(String companyId, String approvedBy) {
        DeletionRequestResponse response = new DeletionRequestResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        CompanyDeletionRequest request = deletionRequestRepository
                .findByCompanyIdAndStatus(companyId, "PENDING").orElse(null);
        if (request == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        if (!"PENDING".equals(request.getStatus())) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_DELETION_ALREADY_PROCESSED);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        if (request.getRequestedBy().equals(approvedBy)) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_CANNOT_APPROVE_OWN_REQUEST);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        request.setStatus("APPROVED");
        request.setDecidedBy(approvedBy);
        request.setDecidedAt(LocalDateTime.now());
        deletionRequestRepository.save(request);

        log.info("Deletion request approved for company {} by {}", companyId, approvedBy);

        DeleteCompanyResponse deleteResult = softDeleteCompanyAndChildren(companyId, approvedBy);

        response.setStatusCode(deleteResult.getStatusCode());
        response.setMessage(deleteResult.getStatusCode().equals(ResponseConstants.OK)
                ? ResponseConstants.MSG_DELETION_REQUEST_APPROVED
                : deleteResult.getMessage());
        response.setRequestId(request.getId());
        response.setCompanyId(companyId);
        response.setCompanyName(company.getNameJa());
        response.setRequestedBy(request.getRequestedBy());
        response.setRequestedAt(request.getRequestedAt());
        response.setReason(request.getReason());
        response.setStatus("APPROVED");
        response.setDecidedBy(approvedBy);
        response.setDecidedAt(request.getDecidedAt());

        return response;
    }

    @Transactional
    public DeletionRequestResponse rejectDeletionRequest(String companyId, String rejectedBy, String rejectionReason) {
        DeletionRequestResponse response = new DeletionRequestResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        CompanyDeletionRequest request = deletionRequestRepository
                .findByCompanyIdAndStatus(companyId, "PENDING").orElse(null);
        if (request == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        if (!"PENDING".equals(request.getStatus())) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_DELETION_ALREADY_PROCESSED);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        request.setStatus("REJECTED");
        request.setDecidedBy(rejectedBy);
        request.setDecidedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);
        deletionRequestRepository.save(request);

        log.info("Deletion request rejected for company {} by {} reason: {}", companyId, rejectedBy, rejectionReason);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_DELETION_REQUEST_REJECTED);
        response.setRequestId(request.getId());
        response.setCompanyId(companyId);
        response.setCompanyName(company.getNameJa());
        response.setRequestedBy(request.getRequestedBy());
        response.setRequestedAt(request.getRequestedAt());
        response.setReason(request.getReason());
        response.setStatus("REJECTED");
        response.setDecidedBy(rejectedBy);
        response.setDecidedAt(request.getDecidedAt());
        response.setRejectionReason(rejectionReason);

        return response;
    }

    @Transactional
    public DeleteCompanyResponse softDeleteCompanyAndChildren(String companyId, String deletedBy) {
        DeleteCompanyResponse response = new DeleteCompanyResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (company.getDeletedAt() != null) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_COMPANY_ALREADY_DELETED);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
            return response;
        }

        try {
            log.info("Soft deleting company {} and all related data by {}", companyId, deletedBy);

            reductionResultRepository.softDeleteByCompanyId(companyId, deletedBy);
            reductionResultTotalRepository.softDeleteByCompanyId(companyId, deletedBy);
            recordCancelRequestRepository.softDeleteByCompanyId(companyId, deletedBy);

            siteR002EquipmentFileRepository.softDeleteByCompanyId(companyId, deletedBy);
            siteR002MemberDataRepository.softDeleteByCompanyId(companyId, deletedBy);

            siteProjectRelationRepository.softDeleteByCompanyId(companyId, deletedBy);

            siteRepository.softDeleteByCompanyId(companyId, deletedBy);
            participatingUserRepository.softDeleteByCompanyId(company.getId(), deletedBy);

            participatingCompanyRepository.softDeleteByCompanyId(companyId, deletedBy);

            log.info("Successfully soft deleted company {} and all related data", companyId);

            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.MSG_DELETE_SUCCESS);
            response.setCompanyId(companyId);
            response.setCompanyName(company.getNameJa());
        } catch (Exception e) {
            log.error("Failed to soft delete company {}: {}", companyId, e.getMessage(), e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        return response;
    }
}
