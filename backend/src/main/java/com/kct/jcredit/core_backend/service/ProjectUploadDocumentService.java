package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.ProjectUploadDocumentRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectUploadDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProjectUploadDocumentService {

    ProjectUploadDocumentResponse processUpload(String projectId, 
                                                MultipartFile fileRegistrationCert, 
                                                MultipartFile fileValidationReport, 
                                                MultipartFile fileVerificationReport, 
                                                String username);

    ProjectUploadDocumentResponse getDocumentsByProjectId(String projectId);

    ProjectUploadDocumentResponse updateStatus(String projectId, ProjectUploadDocumentRequest request);
}