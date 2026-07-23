package com.kct.jcredit.core_backend.service.batch.impl;

import com.kct.jcredit.core_backend.dto.response.SiteReductionResultItemDto;
import com.kct.jcredit.core_backend.dto.response.SiteReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultDataDto;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryItemDto;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryResponse;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.service.batch.GetBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.entity.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetBatchServiceImpl implements GetBatchService {

    private final EnR002ReductionResultRepository reductionResultRepository;
    private final EnR002ReductionResultTotalRepository reductionResultTotalRepository;
    private final BatchProcessHistoryRepository batchProcessHistoryRepository;
    private final SiteRepository siteRepository;

    @Override
    public BatchProcessHistoryResponse getBatchProcessHistory(Integer page, Integer limit, String nextPageToken) {
        // Set defaults
        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100; // Max limit
        }

        // Create pagination object
        PaginationDetailResponse pagination = new PaginationDetailResponse();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setNextPageToken(nextPageToken);

        // Fetch data with pagination
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("startTime").descending());
        Page<BatchProcessHistory> pageResult = batchProcessHistoryRepository.findAll(pageable);

        List<BatchProcessHistoryItemDto> items = pageResult.getContent().stream()
            .map(history -> BatchProcessHistoryItemDto.builder()
                .id(history.getId())
                .isActive(history.getIsActive())
                .createdAt(history.getCreatedAt())
                .createdBy(history.getCreatedBy())
                .updatedAt(history.getUpdatedAt())
                .updatedBy(history.getUpdatedBy())
                .startTime(history.getStartTime())
                .endTime(history.getEndTime())
                .status(history.getStatus())
                .claimedBy(history.getClaimedBy())
                .claimedAt(history.getClaimedAt())
                .build())
            .collect(Collectors.toList());

        // Update pagination details
        pagination.setTotalItems(pageResult.getTotalElements());
        pagination.setTotalPages(pageResult.getTotalPages());
        pagination.setHasMore(pageResult.hasNext());

        return BatchProcessHistoryResponse.builder()
            .items(items)
            .pagination(pagination)
            .build();
    }

    @Override
    public SiteReductionResultResponse getSiteReductionResult(String siteId, Integer projectId) {
        // Fetch reduction result from database
        Site site = siteRepository.findByPublicId(siteId).orElse(null);
        if (site == null) {
            return SiteReductionResultResponse.builder()
                .items(new ArrayList<>())
                .build();
        }
        
        List<EnR002ReductionResult> reductionResults = reductionResultRepository
            .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(projectId, site.getId());
        
        List<SiteReductionResultItemDto> items = new ArrayList<>();
        if (reductionResults.isEmpty()) {
            return SiteReductionResultResponse.builder()
                .items(items)
                .build();
        }
        for (EnR002ReductionResult reductionResult : reductionResults) {
            SiteReductionResultItemDto itemDto = SiteReductionResultItemDto.builder()
                .id(reductionResult.getId())
                .isActive(reductionResult.getIsActive())
                .createdAt(reductionResult.getCreatedAt())
                .createdBy(reductionResult.getCreatedBy())
                .updatedAt(reductionResult.getUpdatedAt())
                .updatedBy(reductionResult.getUpdatedBy())
                .batchId(reductionResult.getBatchId())
                .processingDate(reductionResult.getProcessingDate())
                .projectId(reductionResult.getProjectId())
                .startTime(reductionResult.getStartTime())
                .endTime(reductionResult.getEndTime())
                .siteId(reductionResult.getSiteId())
                .empj(reductionResult.getEmpj())
                .empjM(reductionResult.getEmpjM())
                .empjS(reductionResult.getEmpjS())
                .empjSControl(reductionResult.getEmpjSControl())
                .empjSBattery(reductionResult.getEmpjSBattery())
                .deletedAt(reductionResult.getDeletedAt())
                .deletedBy(reductionResult.getDeletedBy())
                .build();
            
            items.add(itemDto);
        }
        
        return SiteReductionResultResponse.builder()
            .items(items)
            .build();
    }

    @Override
    public ProjectReductionResultResponse getProjectReductionResult(Integer projectId) {
        // Fetch project total reduction result from database
        Optional<EnR002ReductionResultTotal> reductionResultTotal = reductionResultTotalRepository
            .findByProjectId(projectId);
        
        ProjectReductionResultResponse response;
        
        if (reductionResultTotal.isPresent()) {
            EnR002ReductionResultTotal total = reductionResultTotal.get();
            
            ProjectReductionResultDataDto dataDto = ProjectReductionResultDataDto.builder()
                .id(total.getId())
                .isActive(total.getIsActive())
                .createdAt(total.getCreatedAt())
                .createdBy(total.getCreatedBy())
                .updatedAt(total.getUpdatedAt())
                .updatedBy(total.getUpdatedBy())
                .batchId(total.getBatchId())
                .processingDate(total.getProcessingDate())
                .projectId(total.getProjectId())
                .startTime(total.getStartTime())
                .endTime(total.getEndTime())
                .empj(total.getEmpj())
                .empjM(total.getEmpjM())
                .empjS(total.getEmpjS())
                .empjSControl(total.getEmpjSControl())
                .empjSBattery(total.getEmpjSBattery())
                .embl(total.getEmbl())
                .deletedAt(total.getDeletedAt())
                .deletedBy(total.getDeletedBy())
                .build();
            
            response = ProjectReductionResultResponse.builder()
                .statusCode("OK")
                .data(dataDto)
                .build();
        } else {
            response = ProjectReductionResultResponse.builder()
                .statusCode("OK")
                .data(null)
                .build();
        }
        
        return response;
    }

}
