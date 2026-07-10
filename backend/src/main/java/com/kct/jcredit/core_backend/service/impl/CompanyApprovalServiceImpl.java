package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyApprovalServiceImpl implements CompanyApprovalService {

    private static final int DEFAULT_PASSWORD_LENGTH = 12;
    private final ParticipatingCompanyRepository companyRepository;
    private final ParticipatingUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final CompanyLifecycleService companyLifecycleService;

    /**
     * Validates approval preconditions, creates the master user, and activates the company.
     */
    @Override
    @Transactional
    public ApproveCompanyResponse approveCompany(String companyId) {
        ApproveCompanyResponse response = new ApproveCompanyResponse();

        ParticipatingCompany company = companyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (company.getCompanyStatus() != CompanyStatus.PENDING) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_COMPANY_NOT_PENDING);
            return response;
        }

        if (userRepository.findByCompany_IdAndRole(company.getId(), RoleConstants.MASTER).isPresent()) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_MASTER_USER_EXISTS);
            return response;
        }

        String masterUsername = company.getEmail();
        if (userRepository.existsByUsername(masterUsername)) {
            response.setStatusCode(ResponseConstants.USED_USERNAME);
            response.setMessage(ResponseConstants.MSG_USED_USERNAME);
            return response;
        }

        if (userRepository.existsByEmail(masterUsername)) {
            response.setStatusCode(ResponseConstants.USED_EMAIL);
            response.setMessage(ResponseConstants.MSG_USED_EMAIL);
            return response;
        }

        String plainPassword = generateSecurePassword(DEFAULT_PASSWORD_LENGTH);

        ParticipatingUser masterUser = ParticipatingUser.builder()
                .username(masterUsername)
                .firstName(company.getRepFirstName())
                .firstNameKana(company.getRepFirstNameKana())
                .lastName(company.getRepLastName())
                .lastNameKana(company.getRepLastNameKana())
                .email(company.getEmail())
                .phoneNumber(company.getRepPhoneNumber())
                .password(passwordEncoder.encode(plainPassword))
                .role(RoleConstants.MASTER)
                .userPosition(company.getRepPosition())
                .loginAttempts(0)
                .accountLockedUntil(null)
                .site(null)
                .company(company)
                .build();

        try {
            companyLifecycleService.activatePendingCompany(company);
            userRepository.save(masterUser);
            notificationService.sendMasterAccountCredentials(company, masterUsername, plainPassword);
        } catch (Exception ex) {
            log.error("Failed to send master credentials email for company {}", companyId, ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_APPROVAL_EMAIL_FAILED);
            return response;
        }

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_COMPANY_APPROVED);
        response.setCompanyId(companyId);
        response.setMasterUsername(masterUsername);
        return response;
    }

    /**
     * Validates rejection preconditions and moves the company to the rejected state.
     */
    @Override
    @Transactional
    public RejectCompanyResponse rejectCompany(String companyId, String rejectionReason) {
        RejectCompanyResponse response = new RejectCompanyResponse();

        ParticipatingCompany company = companyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (company.getCompanyStatus() != CompanyStatus.PENDING) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_COMPANY_NOT_PENDING);
            return response;
        }

        try {
            companyLifecycleService.rejectPendingCompany(company);
            notificationService.sendCompanyRejectionNotification(company, rejectionReason);
        } catch (Exception ex) {
            log.error("Failed to reject company {}", companyId, ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_COMPANY_REJECT_FAILED);
            return response;
        }

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_COMPANY_REJECTED);
        response.setCompanyId(companyId);
        return response;
    }

    /**
     * Generates a policy-compliant random password for the first master account.
     */
    private String generateSecurePassword(int length) {
        int safeLength = Math.max(length, 8);
        SecureRandom random = new SecureRandom();
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String symbols = "!@#$%^&*()-_=+[]{}<>?";

        List<Character> chars = new ArrayList<>();
        chars.add(upper.charAt(random.nextInt(upper.length())));
        chars.add(lower.charAt(random.nextInt(lower.length())));
        chars.add(digits.charAt(random.nextInt(digits.length())));
        chars.add(symbols.charAt(random.nextInt(symbols.length())));

        String all = upper + lower + digits + symbols;
        while (chars.size() < safeLength) {
            chars.add(all.charAt(random.nextInt(all.length())));
        }

        Collections.shuffle(chars, random);
        StringBuilder builder = new StringBuilder();
        for (char c : chars) {
            builder.append(c);
        }
        return builder.toString();
    }
}
