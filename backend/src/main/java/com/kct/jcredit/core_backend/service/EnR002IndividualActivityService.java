package com.kct.jcredit.core_backend.service;

import java.util.List;

import com.kct.jcredit.core_backend.enums.CompletenessStatus;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.PeriodStatus;
import com.kct.jcredit.core_backend.dto.response.RowDetailActivityResponse;
import com.kct.jcredit.core_backend.dto.response.RowSummaryActivityResponse;
import com.kct.jcredit.core_backend.dto.response.PeriodSummaryResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectGroupResponse;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;

import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;
import com.kct.jcredit.core_backend.service.impl.EnR002IndividualActivityServiceImpl.CompanyKobetsuKatsudoRequest;
import com.kct.jcredit.core_backend.service.impl.EnR002IndividualActivityServiceImpl.PageResponse;
import com.kct.jcredit.core_backend.service.impl.EnR002IndividualActivityServiceImpl.RowsQueryRequest;


public interface EnR002IndividualActivityService {
 
    /**
     * GET /projects/{projectId}/kobetsu-katsudo/periods
     */
    List<PeriodSummaryResponse> getPeriods(String projectId, String currentUserId);
 
    /**
     * GET /projects/{projectId}/kobetsu-katsudo/periods/{periodId}/rows
     * periodId = id batch (row CLOSED) 
     */
    PageResponse<RowSummaryActivityResponse> getRowsForPeriod(
            String projectId, String periodId, RowsQueryRequest request, String currentUserId);
 
    /**
     * GET /companies/{companyId}/kobetsu-katsudo
     * for explisit operator also
     */
    List<ProjectGroupResponse> getRowsForCompany(
            String companyId, CompanyKobetsuKatsudoRequest request, String currentUserId);
 
    /**
     * GET /kobetsu-katsudo/rows/{rowKey}
     * getter for row detail, including all the information about the row, its project, and its company.
     */
    RowDetailActivityResponse getRowDetail(String rowKey, String currentUserId);
}
 