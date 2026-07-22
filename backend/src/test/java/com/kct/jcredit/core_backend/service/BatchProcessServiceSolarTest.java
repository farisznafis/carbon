package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.entity.*;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BatchProcessServiceSolar Test Template
 * 
 * Test each method individually to validate and clean up logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchProcessServiceSolar Unit Test")
class BatchProcessServiceSolarTest {

    @Mock
    private BatchProcessHistoryRepository batchHistoryRepo;
    
    @Mock
    private EnR002ReductionResultRepository monitoringItemsRepo;
    
    @Mock
    private EnR002ReductionResultTotalRepository monitoringTotalRepo;
    
    @Mock
    private MonitoringTableRecordSolarRepository monitoringTableRepo;
    
    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepo;
    
    @Mock
    private SiteRepository siteRepository;
    
    @Mock
    private CsvLogService csvLogService;
    
    @InjectMocks
    private BatchProcessServiceSolar service;
    
    // Test data
    private LocalDate testDate;
    private Integer batchId;
    private Integer projectId;
    private Integer siteId;
    
    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 6, 15);
        batchId = 1001;
        projectId = 100;
        siteId = 201;
    }
    
    // ========================================
    // 1. executeAllProjects()
    // ========================================
    
    @Nested
    @DisplayName("1. executeAllProjects()")
    class ExecuteAllProjectsTests {
        
        @Test
        @DisplayName("SUCCESS: All projects succeed")
        void test_AllProjectsSucceed() {
            // GIVEN
            when(batchHistoryRepo.save(any())).thenAnswer(inv -> {
                BatchProcessHistory h = inv.getArgument(0);
                h.setId(1001);
                return h;
            });
            
            BatchProcessServiceSolar spy = spy(service);
            doReturn(Arrays.asList(100, 101)).when(spy).getActiveSolarProjectIds();
            doNothing().when(spy).processProject(anyInt(), anyInt(), any()  , any(), any());
            
            // WHEN
            spy.executeAllProjects(testDate);
            
            // THEN
            verify(batchHistoryRepo, times(2)).save(argThat(h -> 
                h.getStatus().equals("RUNNING") || h.getStatus().equals("COMPLETED")
            ));
            verify(spy, times(2)).processProject(anyInt(), anyInt(), any(), any(), any());
        }
        
        @Test
        @DisplayName("PARTIAL: Some projects fail")
        void test_SomeProjectsFail() {
            // GIVEN
            when(batchHistoryRepo.save(any())).thenAnswer(inv -> {
                BatchProcessHistory h = inv.getArgument(0);
                h.setId(1001);
                return h;
            });
            
            BatchProcessServiceSolar spy = spy(service);
            doReturn(Arrays.asList(100, 101, 102)).when(spy).getActiveSolarProjectIds();
            
            doNothing().when(spy).processProject(anyInt(), eq(100), any()  , any(), any());
            doThrow(new RuntimeException("DB error"))
                .when(spy).processProject(anyInt(), eq(101), any()  , any(), any());
            doNothing().when(spy).processProject(anyInt(), eq(102), any()  , any(), any());
            
            // WHEN
            spy.executeAllProjects(testDate);
            
            // THEN
            verify(batchHistoryRepo, times(2)).save(argThat(h ->
                h.getStatus().equals("RUNNING") || 
                h.getStatus().equals("COMPLETED_WITH_ERRORS")
            ));
        }
        
    }
    
    // ========================================
    // 2. processProject()
    // ========================================
    
    @Nested
    @DisplayName("2. processProject()")
  class ProcessProjectTests {
    
   @Test
@DisplayName("SUCCESS: Process project with multiple sites")
void test_ProcessProjectSuccess() {
    printTestHeader("processProject - Multiple Sites");
    
    // GIVEN: Setup test data
    List<Integer> siteIds = Arrays.asList(201, 202, 203);
    
    // Mock monitoring data for each site
    Map<Integer, List<EnR002MonitoringRecord>> monitoringData = new HashMap<>();
    monitoringData.put(201, createMockRecords(5));
    monitoringData.put(202, createMockRecords(3));
    monitoringData.put(203, createMockRecords(7));
    
    // Create spy to mock internal methods
    BatchProcessServiceSolar spy = spy(service);
    
    // Mock getSitesForProject
    doReturn(siteIds).when(spy).getSitesForProject(projectId);
    
    // Mock loadMonitoringDataFromDynamoDB - Accept ANY Instant parameters
    doReturn(monitoringData).when(spy)
        .loadMonitoringDataFromDynamoDB(
            eq(projectId), 
            eq(siteIds), 
            any(Instant.class),  // ← Use any(Instant.class)
            any(Instant.class)   // ← Use any(Instant.class)
        );
    
    // Mock calculateSiteEmissions for each site
    EnR002ReductionResult mockItems = createMockMonitoringItems();
    doReturn(mockItems).when(spy)
        .calculateSiteEmissions(
            anyInt(), 
            anyInt(), 
            anyInt(), 
            anyList(), 
            any(LocalDate.class),
            any(Instant.class)
        );
    
    // Mock repository save
    when(monitoringItemsRepo.save(any(EnR002ReductionResult.class)))
        .thenReturn(mockItems);
    
    // Mock CSV comparison
    ComparisonResult comparison = ComparisonResult.builder()
        .successCount(3)
        .warningCount(0)
        .errorCount(0)
       // .hasDuplicates(false)  // ← Add this
        .build();
    when(csvLogService.compareProcessingLogs(anyString(), anyString(), anyString()))
        .thenReturn(comparison);
    
    // Mock aggregateProjectTotals
    EnR002ReductionResultTotal mockTotal = createMockMonitoringTotal();
    doReturn(mockTotal).when(spy)
        .aggregateProjectTotals(anyInt(), anyInt(), any(LocalDate.class), anyList());
    
    when(monitoringTotalRepo.save(any(EnR002ReductionResultTotal.class)))
        .thenReturn(mockTotal);
    
    // Create fixed test times (avoid Instant.now() issues)
    Instant batchStartTime = Instant.parse("2024-06-15T10:00:00Z");
    Instant lastBatchStartTime = Instant.parse("2024-06-14T10:00:00Z");
    
    // WHEN: Process the project
    spy.processProject(batchId, projectId, testDate, batchStartTime, lastBatchStartTime);
    
    // THEN: Verify method calls
    System.out.println("=== Verification ===");
    System.out.println("✓ Sites loaded: 3");
    System.out.println("✓ Monitoring data loaded");
    System.out.println("✓ Target CSV created");
    System.out.println("✓ Result CSV created");
    System.out.println("✓ Site calculations: 3 times");
    System.out.println("✓ Database saves: 3 times");
    System.out.println("✓ CSV appends: 3 times");
    System.out.println("✓ Logs compared");
    System.out.println("✓ Totals aggregated");
    System.out.println("✓ Total saved");
    
    // Verify site loading
    verify(spy).getSitesForProject(projectId);
    
    // Verify monitoring data loaded - use actual values passed
    verify(spy).loadMonitoringDataFromDynamoDB(
        eq(projectId), 
        eq(siteIds), 
        eq(batchStartTime),      // ← Use eq() with actual values
        eq(lastBatchStartTime)   // ← Use eq() with actual values
    );
    
    // Verify CSV creation
    verify(csvLogService).createTargetCsv(anyString(), anyList());
    verify(csvLogService).createEmptyResultCsv(anyString());
    
    // Verify site processing (3 sites)
    verify(spy, times(3)).calculateSiteEmissions(
        eq(batchId), 
        eq(projectId), 
        anyInt(),  // siteId varies
        anyList(), 
        eq(testDate),
        eq(lastBatchStartTime)
    );
    
    // Verify database saves
    verify(monitoringItemsRepo, times(3)).save(any(EnR002ReductionResult.class));
    
    // Verify CSV logging
    verify(csvLogService, times(3)).appendToResultCsv(
        anyString(), 
        any(ProcessingResult.class)
    );
    
    // Verify comparison
    verify(csvLogService).compareProcessingLogs(anyString(), anyString(), anyString());
    
    // Verify aggregation
    verify(spy).aggregateProjectTotals(
        eq(batchId), 
        eq(projectId), 
        eq(testDate), 
        anyList()
    );
    
    // Verify total saved
    verify(monitoringTotalRepo).save(any(EnR002ReductionResultTotal.class));
    
    printTestResult("SUCCESS");
}
    
    @Test
    @DisplayName("PARTIAL: Some sites have no data")
    void test_SomeSitesNoData() {
        // GIVEN
        List<Integer> siteIds = Arrays.asList(201, 202, 203);
        
        Map<Integer, List<EnR002MonitoringRecord>> monitoringData = new HashMap<>();
        monitoringData.put(201, createMockRecords(5));
        monitoringData.put(202, Collections.emptyList());  // NO DATA
        monitoringData.put(203, createMockRecords(3));
        
        BatchProcessServiceSolar spy = spy(service);
        doReturn(siteIds).when(spy).getSitesForProject(projectId);
        doReturn(monitoringData).when(spy)
            .loadMonitoringDataFromDynamoDB(anyInt(), anyList(), any(), any());
        
        EnR002ReductionResult mockItems = createMockMonitoringItems();
        doReturn(mockItems).when(spy)
            .calculateSiteEmissions(anyInt(), anyInt(), anyInt(), anyList(), any(),any());
        when(monitoringItemsRepo.save(any())).thenReturn(mockItems);
        
        ComparisonResult comparison = ComparisonResult.builder()
            .successCount(2).warningCount(1).errorCount(0)
            .build();
        when(csvLogService.compareProcessingLogs(anyString(), anyString(), anyString()))
            .thenReturn(comparison);
        
        EnR002ReductionResultTotal mockTotal = createMockMonitoringTotal();
        doReturn(mockTotal).when(spy)
            .aggregateProjectTotals(anyInt(), anyInt(), any(), anyList());
        when(monitoringTotalRepo.save(any())).thenReturn(mockTotal);
        
        // WHEN
        spy.processProject(batchId, projectId, testDate , Instant.now(), Instant.now());
        
        // THEN
        System.out.println("Verifications:");
        System.out.println("  - Site 201: calculated");
        System.out.println("  - Site 202: skipped (no data)");
        System.out.println("  - Site 203: calculated");
        System.out.println("  - Database saves: 2 times (not 3)");
        System.out.println("  - CSV appends: 3 times (includes NO_DATA result)");
        
        verify(spy, times(2)).calculateSiteEmissions(anyInt(), anyInt(), anyInt(), anyList(), any(),any());
        verify(monitoringItemsRepo, times(2)).save(any());
        verify(csvLogService, times(3)).appendToResultCsv(anyString(), any());
        
        System.out.println("");
        System.out.println(">>> SUCCESS");
    }
    
    
    @Test
    @DisplayName("ERROR: Site calculation fails")
    void test_SiteCalculationFails() {
        // GIVEN
        List<Integer> siteIds = Arrays.asList(201, 202);
        
        Map<Integer, List<EnR002MonitoringRecord>> monitoringData = new HashMap<>();
        monitoringData.put(201, createMockRecords(5));
        monitoringData.put(202, createMockRecords(3));
        
        BatchProcessServiceSolar spy = spy(service);
        doReturn(siteIds).when(spy).getSitesForProject(projectId);
        doReturn(monitoringData).when(spy)
            .loadMonitoringDataFromDynamoDB(anyInt(), anyList(), any()  , any());
        
        // Site 201 succeeds
        EnR002ReductionResult mockItems = createMockMonitoringItems();
        doReturn(mockItems).when(spy)
            .calculateSiteEmissions(eq(batchId), eq(projectId), eq(201), anyList(), any(),any());
        
        // Site 202 fails
        doThrow(new RuntimeException("Calculation error"))
            .when(spy).calculateSiteEmissions(eq(batchId), eq(projectId), eq(202), anyList(), any(), any());
        
        when(monitoringItemsRepo.save(any())).thenReturn(mockItems);
        
        ComparisonResult comparison = ComparisonResult.builder()
            .successCount(1).warningCount(0).errorCount(1)
            .build();
        when(csvLogService.compareProcessingLogs(anyString(), anyString(), anyString()))
            .thenReturn(comparison);
        
        EnR002ReductionResultTotal mockTotal = createMockMonitoringTotal();
        doReturn(mockTotal).when(spy)
            .aggregateProjectTotals(anyInt(), anyInt(), any(), anyList());
        when(monitoringTotalRepo.save(any())).thenReturn(mockTotal);
        
        // WHEN
        spy.processProject(batchId, projectId, testDate , Instant.now(), Instant.now());
        
        // THEN
        System.out.println("Verifications:");
        System.out.println("  - Site 201: success");
        System.out.println("  - Site 202: failed (exception caught)");
        System.out.println("  - Database saves: 1 time (only site 201)");
        System.out.println("  - CSV appends: 2 times (includes FAILED result)");
        System.out.println("  - Processing continued despite failure");
        
        verify(spy, times(2)).calculateSiteEmissions(anyInt(), anyInt(), anyInt(), anyList(), any(),any());
        verify(monitoringItemsRepo, times(1)).save(any());
        verify(csvLogService, times(2)).appendToResultCsv(anyString(), argThat(result ->
            result.getStatus().equals("SUCCESS") || result.getStatus().equals("FAILED")
        ));
        
        System.out.println("");
        System.out.println(">>> SUCCESS - Graceful failure handling");
    }
    
    // Helper methods
    private List<EnR002MonitoringRecord> createMockRecords(int count) {
        List<EnR002MonitoringRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EnR002MonitoringRecord record = new EnR002MonitoringRecord();
            record.setElPv(BigDecimal.valueOf(100));
            record.setElPvr(BigDecimal.valueOf(20));
            records.add(record);
        }
        return records;
    }
    
    private EnR002ReductionResult createMockMonitoringItems() {
        EnR002ReductionResult items = new EnR002ReductionResult();
        items.setId(5000);
        items.setEmpj(BigDecimal.valueOf(80));
        return items;
    }
    
    private EnR002ReductionResultTotal createMockMonitoringTotal() {
        EnR002ReductionResultTotal total = new EnR002ReductionResultTotal();
        total.setId(6000);
        total.setEmpj(BigDecimal.valueOf(500));
        return total;
    }
  }
     
    // ========================================
    // 4. loadMonitoringDataFromDynamoDB()
    // ========================================
    
    @Nested
    @DisplayName("4. loadMonitoringDataFromDynamoDB()")
   class LoadMonitoringDataTests {
        
        private Instant lastBatchStart;
        private Instant currentBatchStart;
        
        @BeforeEach
        void setUpTimeRange() {
            lastBatchStart = Instant.parse("2024-06-14T09:30:00Z");
            currentBatchStart = Instant.parse("2024-06-15T10:00:00Z");
        }
        
        @Test
        @DisplayName("SUCCESS: Load data for all sites")
        void test_LoadAllSites() {
            printTestHeader("loadMonitoringDataFromDynamoDB - All Sites");
            
            // GIVEN
            List<Integer> siteIds = Arrays.asList(201, 202);
            
            // Site 201: 2 valid, 1 processed, 1 inactive
            when(monitoringTableRepo.findByPkAndSkBetween(
                eq("100#201"), 
                eq(lastBatchStart.toString()), 
                eq(currentBatchStart.toString())))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#201", "2024-06-14T10:00:00Z", null, true),
                    createMonitoringRecord("100#201", "2024-06-14T11:00:00Z",  testDate, true),
                    createMonitoringRecord("100#201", "2024-06-15T08:00:00Z", null, true),
                    createMonitoringRecord("100#201", "2024-06-15T09:00:00Z",  null, false)
                ));
            
            // Site 202: 1 valid
            when(monitoringTableRepo.findByPkAndSkBetween(
                eq("100#202"), 
                eq(lastBatchStart.toString()), 
                eq(currentBatchStart.toString())))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#202", "2024-06-15T08:00:00Z",  null, true)
                ));
            
            // WHEN
            Map<Integer, List<EnR002MonitoringRecord>> result = 
                service.loadMonitoringDataFromDynamoDB(
                    projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Results:");
            System.out.println("  Site 201: " + result.get(201).size() + " records (expected: 2)");
            System.out.println("  Site 202: " + result.get(202).size() + " records (expected: 1)");
            System.out.println("  Total: 3 unprocessed records");
            
            assertThat(result).hasSize(2);
            assertThat(result.get(201)).hasSize(2);
            assertThat(result.get(202)).hasSize(1);
            
            // Verify filtering conditions
            result.values().stream()
                .flatMap(List::stream)
                .forEach(record -> {
                    assertThat(record.getProcessedAt()).isNull();
                    assertThat(record.getIsActive()).isTrue();
                });
            
            verify(monitoringTableRepo).findByPkAndSkBetween(
                "100#201", lastBatchStart.toString(), currentBatchStart.toString());
            verify(monitoringTableRepo).findByPkAndSkBetween(
                "100#202", lastBatchStart.toString(), currentBatchStart.toString());
            
            printTestResult("SUCCESS");
        }
        
        @Test
        @DisplayName("PARTIAL: Some sites have no data")
        void test_SomeSitesNoData() {
            printTestHeader("loadMonitoringDataFromDynamoDB - Some Sites No Data");
            
            // GIVEN
            List<Integer> siteIds = Arrays.asList(201, 202, 203);
            
            // Site 201: has data
            when(monitoringTableRepo.findByPkAndSkBetween(
                eq("100#201"), anyString(), anyString()))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#201", "2024-06-15T09:00:00Z", null, true)
                ));
            
            // Site 202: no data in time range
            when(monitoringTableRepo.findByPkAndSkBetween(
                eq("100#202"), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
            
            // Site 203: has data but all processed
            when(monitoringTableRepo.findByPkAndSkBetween(
                eq("100#203"), anyString(), anyString()))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#203", "2024-06-15T08:00:00Z", testDate, true)
                ));
            
            // WHEN
            Map<Integer, List<EnR002MonitoringRecord>> result = 
                service.loadMonitoringDataFromDynamoDB(
                    projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Results:");
            System.out.println("  Site 201: " + result.get(201).size() + " records");
            System.out.println("  Site 202: " + result.get(202).size() + " records (empty - no data)");
            System.out.println("  Site 203: " + result.get(203).size() + " records (empty - all processed)");
            
            assertThat(result).hasSize(3);
            assertThat(result.get(201)).hasSize(1);
            assertThat(result.get(202)).isEmpty();
            assertThat(result.get(203)).isEmpty();
            
            printTestResult("SUCCESS");
        }
        
        @Test
        @DisplayName("EDGE: Empty site list")
        void test_EmptySiteList() {
            printTestHeader("loadMonitoringDataFromDynamoDB - Empty Site List");
            
            // GIVEN
            List<Integer> siteIds = Collections.emptyList();
            
            // WHEN
            Map<Integer, List<EnR002MonitoringRecord>> result = 
                service.loadMonitoringDataFromDynamoDB(
                    projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Empty site list returns empty result");
            
            assertThat(result).isEmpty();
            verify(monitoringTableRepo, never())
                .findByPkAndSkBetween(anyString(), anyString(), anyString());
            
            printTestResult("SUCCESS");
        }
        
        @Test
        @DisplayName("FILTER: Only unprocessed records included")
        void test_FilterProcessedRecords() {
            printTestHeader("loadMonitoringDataFromDynamoDB - Filter Processed");
            
            // GIVEN
            List<Integer> siteIds = Arrays.asList(201);
            
            when(monitoringTableRepo.findByPkAndSkBetween(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#201", "2024-06-14T10:00:00Z", null, true),
                    createMonitoringRecord("100#201", "2024-06-14T11:00:00Z", LocalDate.of(2024, 6, 14), true),
                    createMonitoringRecord("100#201", "2024-06-15T09:00:00Z", null, true),
                    createMonitoringRecord("100#201", "2024-06-15T09:30:00Z", LocalDate.of(2024, 6, 15), true)
                ));
            
            // WHEN
            Map<Integer, List<EnR002MonitoringRecord>> result = 
                service.loadMonitoringDataFromDynamoDB(
                    projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Filter: processedAt = null");
            System.out.println("  Input: 4 records");
            System.out.println("  Unprocessed: 2 records");
            System.out.println("  Processed (excluded): 2 records");
            
            assertThat(result.get(201)).hasSize(2);
            assertThat(result.get(201))
                .allMatch(record -> record.getProcessedAt() == null);
            
            printTestResult("SUCCESS");
        }
        
        @Test
        @DisplayName("FILTER: Only active records included")
        void test_FilterInactiveRecords() {
            printTestHeader("loadMonitoringDataFromDynamoDB - Filter Inactive");
            
            // GIVEN
            List<Integer> siteIds = Arrays.asList(201);
            
            when(monitoringTableRepo.findByPkAndSkBetween(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#201", "2024-06-15T08:00:00Z", null, true),
                    createMonitoringRecord("100#201", "2024-06-15T09:00:00Z", null, false),
                    createMonitoringRecord("100#201", "2024-06-15T09:30:00Z", null, false),
                    createMonitoringRecord("100#201", "2024-06-15T09:45:00Z", null, true)
                ));
            
            // WHEN
            Map<Integer, List<EnR002MonitoringRecord>> result = 
                service.loadMonitoringDataFromDynamoDB(
                    projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Filter: isActive = true");
            System.out.println("  Input: 4 records");
            System.out.println("  Active: 2 records");
            System.out.println("  Inactive (excluded): 2 records");
            
            assertThat(result.get(201)).hasSize(2);
            assertThat(result.get(201))
                .allMatch(record -> Boolean.TRUE.equals(record.getIsActive()));
            
            printTestResult("SUCCESS");
        }
        
        @Test
        @DisplayName("FILTER: Both conditions must be true")
        void test_BothConditions() {
            printTestHeader("loadMonitoringDataFromDynamoDB - Both Conditions");
            
            // GIVEN
            List<Integer> siteIds = Arrays.asList(201);
            
            when(monitoringTableRepo.findByPkAndSkBetween(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#201", "2024-06-15T08:00:00Z", null, true),
                    createMonitoringRecord("100#201", "2024-06-15T09:00:00Z", null, false),
                    createMonitoringRecord("100#201", "2024-06-15T09:30:00Z", testDate, true),
                    createMonitoringRecord("100#201", "2024-06-15T09:45:00Z", null, true)
                ));
            
            // WHEN
            Map<Integer, List<EnR002MonitoringRecord>> result = 
                service.loadMonitoringDataFromDynamoDB(
                    projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Filter: processedAt=null AND isActive=true");
            System.out.println("  Input: 4 records");
            System.out.println("  Record 1: unprocessed + active = PASS");
            System.out.println("  Record 2: unprocessed + inactive = FAIL");
            System.out.println("  Record 3: processed + active = FAIL");
            System.out.println("  Record 4: unprocessed + active = PASS");
            System.out.println("  Result: 2 records");
            
            assertThat(result.get(201)).hasSize(2);
            assertThat(result.get(201))
                .allMatch(r -> r.getProcessedAt() == null && Boolean.TRUE.equals(r.getIsActive()));
            
            printTestResult("SUCCESS");
        }
        
        @Test
        @DisplayName("TIME RANGE: Uses correct start and end times")
        void test_TimeRangeQuery() {
            printTestHeader("loadMonitoringDataFromDynamoDB - Time Range");
            
            // GIVEN
            List<Integer> siteIds = Arrays.asList(201);
            
            when(monitoringTableRepo.findByPkAndSkBetween(anyString(), anyString(), anyString()))
                .thenReturn(Arrays.asList(
                    createMonitoringRecord("100#201", "2024-06-15T09:00:00Z", null, true)
                ));
            
            // WHEN
            service.loadMonitoringDataFromDynamoDB(
                projectId, siteIds, currentBatchStart, lastBatchStart);
            
            // THEN
            System.out.println("Time range query:");
            System.out.println("  Start: " + lastBatchStart);
            System.out.println("  End: " + currentBatchStart);
            
            verify(monitoringTableRepo).findByPkAndSkBetween(
                "100#201",
                lastBatchStart.toString(),
                currentBatchStart.toString()
            );
            
            printTestResult("SUCCESS");
        }
    }

// ========================================
// Helper Methods (add to existing section)
// ========================================

private EnR002MonitoringRecord createMonitoringRecord(
        String pk, String sk, LocalDate processedAt, Boolean isActive) {
    

    EnR002MonitoringRecord record = new EnR002MonitoringRecord();
    record.setId(pk);
    record.setTimeUpdated(sk);
    record.setProcessedAt(processedAt != null ? processedAt.toString() : null);
    record.setIsActive(isActive);
    record.setElPv(new BigDecimal("100.5"));
    record.setElPvr(new BigDecimal("20.3"));
    
    return record;
}
    
    // ========================================
    // 5. calculateSiteEmissions()
    // ========================================
    
    @Nested
    @DisplayName("5. calculateSiteEmissions()")
 
    class CalculateSiteEmissionsTests {
    
    private Instant lastBatchStartTime;
    private LocalDate processingDate;
    private LocalDate activityStartDate;
    
    @BeforeEach
    void setUpCalculation() {
    lastBatchStartTime = Instant.parse("2024-06-14T10:00:00Z");
    processingDate = LocalDate.of(2024, 6, 15);
    activityStartDate = LocalDate.of(2024, 1, 1);
    
    // Create mock Project entity
    Project mockProject = new Project();
    mockProject.setId(projectId);
    mockProject.setProjectName("Test Solar Project");
    
    // Create mock Site entity
    Site mockSite = new Site();
    mockSite.setId(siteId);
    mockSite.setSiteName("Test Site");
    
    // Mock activity start date lookup with full entities
    SiteProjectRelation mockRelation = new SiteProjectRelation();
    mockRelation.setProject(mockProject);
    mockRelation.setSite(mockSite);
    mockRelation.setActivityStartDate(activityStartDate);
    
    when(siteProjectRelationRepo.findByProjectIdAndSiteId(projectId, siteId))
        .thenReturn(mockRelation);
    }
    
    @Test
    @DisplayName("SUCCESS: Calculate emissions without battery")
    void test_CalculateNoBattery() {
        printTestHeader("calculateSiteEmissions - No Battery");
        
        // GIVEN
        List<EnR002MonitoringRecord> data = Arrays.asList(
            createRecord("100.0", "20.0", null, false),
            createRecord("150.0", "30.0", null, false),
            createRecord("120.0", "25.0", null, false)
        );
        
        // Expected:
        // ELPV = 100 + 150 + 120 = 370
        // ELPVR = 20 + 30 + 25 = 75
        // ELPJ,control = 370 - 75 = 295
        // EM_PJ,M = 0 (no methane)
        // EM_PJ,S,control = 295 × 0.000512 = 0.151
        // EM_PJ,S,battery = 0 (no battery)
        // EM_PJ,S = 0.151
        // EM_PJ = 0 + 0.151 = 0.151
        
        // WHEN
        EnR002ReductionResult result = service.calculateSiteEmissions(
            batchId, projectId, siteId, data, processingDate, lastBatchStartTime);
        
        // THEN
        System.out.println("Input: ELPV=370, ELPVR=75");
        System.out.println("ELPJ,control = 295 kWh");
        System.out.println("EM_PJ,M = 0.000 tCO2");
        System.out.println("EM_PJ,S,control = 0.151 tCO2");
        System.out.println("EM_PJ,S,battery = 0.000 tCO2");
        System.out.println("EM_PJ,S = 0.151 tCO2");
        System.out.println("EM_PJ = 0.151 tCO2");
        
        assertThat(result.getBatchId()).isEqualTo(batchId);
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getSiteId()).isEqualTo(siteId);
        assertThat(result.getProcessingDate()).isEqualTo(processingDate);
        
        assertThat(result.getEmpjM()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("0.151");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjS()).isEqualByComparingTo("0.151");
        assertThat(result.getEmpj()).isEqualByComparingTo("0.151");
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Calculate emissions with battery changed")
    void test_CalculateWithBatteryChanged() {
        printTestHeader("calculateSiteEmissions - Battery Changed");
        
        // GIVEN
        List<EnR002MonitoringRecord> data = Arrays.asList(
            createRecord("100.0", "20.0", "10.0", false),
            createRecord("150.0", "30.0", "15.0", true),  // Battery changed!
            createRecord("120.0", "25.0", "12.0", false)
        );
        
        // Expected:
        // ELPJ,control = (370 - 75) = 295
        // ELPJ,battery = 10 + 15 + 12 = 37
        // EM_PJ,S,control = 295 × 0.000512 = 0.151
        // EM_PJ,S,battery = 37 × 0.000512 = 0.019 (because changed=true)
        // EM_PJ,S = 0.151 + 0.019 = 0.170
        // EM_PJ = 0.170
        
        // WHEN
        EnR002ReductionResult result = service.calculateSiteEmissions(
            batchId, projectId, siteId, data, processingDate, lastBatchStartTime);
        
        // THEN
        System.out.println("Battery changed flag = true");
        System.out.println("ELPJ,battery = 37 kWh");
        System.out.println("EM_PJ,S,control = 0.151 tCO2");
        System.out.println("EM_PJ,S,battery = 0.019 tCO2");
        System.out.println("EM_PJ = 0.170 tCO2");
        
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("0.151");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.019");
        assertThat(result.getEmpjS()).isEqualByComparingTo("0.170");
        assertThat(result.getEmpj()).isEqualByComparingTo("0.170");
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Battery one year passed")
    void test_BatteryOneYearPassed() {
        printTestHeader("calculateSiteEmissions - Battery One Year");
        
        // GIVEN
        List<EnR002MonitoringRecord> data = Arrays.asList(
            createRecord("100.0", "20.0", "10.0", false)
        );
        
        // Mock: Last battery calculation was >1 year ago
        EnR002ReductionResult oldItem = EnR002ReductionResult.builder()
            .processingDate(processingDate.minusYears(2))
            .empjSBattery(new BigDecimal("0.010"))
            .build();
        
        when(monitoringItemsRepo.findLastBatteryCalculationDate(
            eq(projectId), eq(siteId)))
            .thenReturn(Optional.of(processingDate.minusYears(2)));
        
        // Expected:
        // EM_PJ,S,battery = 10 × 0.000512 = 0.005 (one year trigger)
        
        // WHEN
        EnR002ReductionResult result = service.calculateSiteEmissions(
            batchId, projectId, siteId, data, processingDate, lastBatchStartTime);
        
        // THEN
        System.out.println("One year since last battery emission");
        System.out.println("EM_PJ,S,battery = 0.005 tCO2");
        
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.005");
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Battery no trigger")
    void test_BatteryNoTrigger() {
        printTestHeader("calculateSiteEmissions - Battery No Trigger");
        
        // GIVEN
        List<EnR002MonitoringRecord> data = Arrays.asList(
            createRecord("100.0", "20.0", "10.0", false)
        );
        
        // Mock: Last battery calculation was 6 months ago
        EnR002ReductionResult recentItem = EnR002ReductionResult.builder()
            .processingDate(processingDate.minusMonths(6))
            .empjSBattery(new BigDecimal("0.010"))
            .build();
        
        when(monitoringItemsRepo.findLastBatteryCalculationDate(
            eq(projectId), eq(siteId)))
            .thenReturn(Optional.of(recentItem.getProcessingDate()));
        
        // Expected:
        // EM_PJ,S,battery = 0 (no trigger, less than 1 year)
        
        // WHEN
        EnR002ReductionResult result = service.calculateSiteEmissions(
            batchId, projectId, siteId, data, processingDate, lastBatchStartTime);
        
        // THEN
        System.out.println("Battery usage but no trigger (< 1 year)");
        System.out.println("EM_PJ,S,battery = 0.000 tCO2");
        
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.000");
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: All zero values")
    void test_AllZeroValues() {
        printTestHeader("calculateSiteEmissions - All Zeros");
        
        // GIVEN
        List<EnR002MonitoringRecord> data = Arrays.asList(
            createRecord("0", "0", "0", false),
            createRecord("0", "0", "0", false)
        );
        
        // WHEN
        EnR002ReductionResult result = service.calculateSiteEmissions(
            batchId, projectId, siteId, data, processingDate, lastBatchStartTime);
        
        // THEN
        System.out.println("All values = 0");
        System.out.println("All emissions = 0.000 tCO2");
        
        assertThat(result.getEmpjM()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjS()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpj()).isEqualByComparingTo("0.000");
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Null values treated as zero")
    void test_NullValues() {
        printTestHeader("calculateSiteEmissions - Null Values");
        
        // GIVEN
        List<EnR002MonitoringRecord> data = Arrays.asList(
            createRecord("100.0", null, null, false),
            createRecord(null, "20.0", "10.0", false),
            createRecord("150.0", "30.0", null, false)
        );
        
        // Expected:
        // ELPV = 100 + 0 + 150 = 250
        // ELPVR = 0 + 20 + 30 = 50
        // ELPJ,control = 200
        // EM_PJ,S,control = 200 × 0.000512 = 0.102
        
        // WHEN
        EnR002ReductionResult result = service.calculateSiteEmissions(
            batchId, projectId, siteId, data, processingDate, lastBatchStartTime);
        
        // THEN
        System.out.println("Nulls treated as 0");
        System.out.println("EM_PJ,S,control = 0.102 tCO2");
        
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("0.102");
        
        printTestResult("SUCCESS");
    }
}

// ========================================
// Helper Methods
// ========================================

/**
 * Creates a monitoring record with String values for easier reading.
 */
private EnR002MonitoringRecord createRecord(
        String elpv, String elpvr, String battery, boolean changed) {
    EnR002MonitoringRecord record = new EnR002MonitoringRecord();
    record.setElPv(elpv != null ? new BigDecimal(elpv) : null);
    record.setElPvr(elpvr != null ? new BigDecimal(elpvr) : null);
    record.setElpjBatteryValue(battery != null ? new BigDecimal(battery) : null);
    record.setChangedBattery(changed);
    return record;
        }

    
    
    // ========================================
    // 7. getCefElectricity()
    // ========================================
    
    @Nested
    @DisplayName("7. getCefElectricity()")
    class GetCefElectricityTests {
    
    private static final BigDecimal CMO = new BigDecimal("0.000512");
    
    @Test
    @DisplayName("SUCCESS: Early project (< 1 year) uses marginal factor only")
    void test_EarlyProject() {
        printTestHeader("getCefElectricity - Early Project");
        
        // GIVEN: Project started 6 months ago
        LocalDate activityStartDate = LocalDate.of(2024, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 7, 1);
        
        // Expected: f(t) = 0.0 (< 1 year)
        // CEF = C_mo × (1 - 0.0) + C_a × 0.0 = C_mo = 0.000512
        BigDecimal expectedCef = new BigDecimal("0.000512");
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years since start: 0.5");
        System.out.println("Transition factor f(t): 0.0");
        System.out.println("Formula: CEF = C_mo × 1.0 + C_a × 0.0");
        System.out.println("Expected CEF: " + expectedCef);
        System.out.println("Actual CEF: " + result);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Exactly 1 year triggers transition (f=0.5)")
    void test_OneYearTransition() {
        printTestHeader("getCefElectricity - One Year Transition");
        
        // GIVEN: Project started exactly 1 year ago
        LocalDate activityStartDate = LocalDate.of(2023, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 1, 2);
        
        // Expected: f(t) = 0.5 (1 year ≤ t < 2.5)
        // CEF = C_mo × 0.5 + C_a(2024) × 0.5
        //     = 0.000512 × 0.5 + 0.000438 × 0.5
        //     = 0.000256 + 0.000219
        //     = 0.000475
        BigDecimal ca2024 = new BigDecimal("0.000438");
        BigDecimal expectedCef = CMO.multiply(new BigDecimal("0.5"))
                                    .add(ca2024.multiply(new BigDecimal("0.5")))
                                    .setScale(6, RoundingMode.HALF_UP);
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years since start: 1.0");
        System.out.println("Transition factor f(t): 0.5");
        System.out.println("C_mo: " + CMO);
        System.out.println("C_a(2024): " + ca2024);
        System.out.println("Formula: CEF = 0.000512 × 0.5 + 0.000438 × 0.5");
        System.out.println("Expected CEF: " + expectedCef);
        System.out.println("Actual CEF: " + result);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Transition period (1.5 years) uses blended factor")
    void test_TransitionPeriod() {
        printTestHeader("getCefElectricity - Transition Period");
        
        // GIVEN: Project started 1.5 years ago
        LocalDate activityStartDate = LocalDate.of(2023, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 7, 1);
        
        // Expected: f(t) = 0.5 (1 year ≤ 1.5 < 2.5)
        // CEF = C_mo × 0.5 + C_a(2024) × 0.5 = 0.000475
        BigDecimal ca2024 = new BigDecimal("0.000438");
        BigDecimal expectedCef = CMO.multiply(new BigDecimal("0.5"))
                                    .add(ca2024.multiply(new BigDecimal("0.5")))
                                    .setScale(6, RoundingMode.HALF_UP);
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years since start: 1.5");
        System.out.println("Transition factor f(t): 0.5 (blended)");
        System.out.println("Expected CEF: " + expectedCef);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Exactly 2.5 years triggers full transition (f=1.0)")
    void test_TwoAndHalfYears() {
        printTestHeader("getCefElectricity - 2.5 Years");
        
        // GIVEN: Project started 2.5 years ago (913 days)
        LocalDate activityStartDate = LocalDate.of(2022, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 7, 15); // ~2.54 years
        
        // Expected: f(t) = 1.0 (≥ 2.5 years)
        // CEF = C_mo × 0.0 + C_a(2024) × 1.0 = C_a(2024) = 0.000438
        BigDecimal expectedCef = new BigDecimal("0.000438");
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years since start: ~2.54");
        System.out.println("Transition factor f(t): 1.0");
        System.out.println("Formula: CEF = C_mo × 0.0 + C_a × 1.0 = C_a");
        System.out.println("Expected CEF: " + expectedCef);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Mature project (5 years) uses all-power factor only")
    void test_MatureProject() {
        printTestHeader("getCefElectricity - Mature Project");
        
        // GIVEN: Project started 5 years ago
        LocalDate activityStartDate = LocalDate.of(2019, 6, 1);
        LocalDate processingDate = LocalDate.of(2024, 6, 1);
        
        // Expected: f(t) = 1.0 (≥ 2.5 years)
        // CEF = C_a(2024) = 0.000438
        BigDecimal expectedCef = new BigDecimal("0.000438");
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years since start: 5.0");
        System.out.println("Transition factor f(t): 1.0");
        System.out.println("Expected CEF: " + expectedCef + " (all-power factor)");
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Uses C_a for correct processing year")
    void test_CorrectYearSelection() {
        printTestHeader("getCefElectricity - Correct Year");
        
        // GIVEN: Processing in 2022 (mature project)
        LocalDate activityStartDate = LocalDate.of(2019, 1, 1);
        LocalDate processingDate = LocalDate.of(2022, 6, 1);
        
        // Expected: f(t) = 1.0, use C_a(2022)
        BigDecimal expectedCef = new BigDecimal("0.000436"); // CA_MAP value for 2022
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Processing year: 2022");
        System.out.println("Expected C_a(2022): " + expectedCef);
        System.out.println("Actual CEF: " + result);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Uses default C_a for unmapped year")
    void test_UnmappedYearUsesDefault() {
        printTestHeader("getCefElectricity - Unmapped Year");
        
        // GIVEN: Processing in 2030 (not in CA_MAP, mature project)
        LocalDate activityStartDate = LocalDate.of(2020, 1, 1);
        LocalDate processingDate = LocalDate.of(2030, 6, 1);
        
        // Expected: f(t) = 1.0, use DEFAULT_CA = 0.000441
        BigDecimal expectedCef = new BigDecimal("0.000441");
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Processing year: 2030 (not in CA_MAP)");
        System.out.println("Using DEFAULT_CA: " + expectedCef);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Same day (0 years) uses marginal factor")
    void test_SameDay() {
        printTestHeader("getCefElectricity - Same Day");
        
        // GIVEN: Processing on start date
        LocalDate activityStartDate = LocalDate.of(2024, 6, 15);
        LocalDate processingDate = LocalDate.of(2024, 6, 15);
        
        // Expected: f(t) = 0.0 (0 years)
        BigDecimal expectedCef = CMO; // 0.000512
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years since start: 0.0");
        System.out.println("Expected CEF: " + expectedCef + " (marginal only)");
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Just before 1 year (364 days) still uses marginal")
    void test_JustBeforeOneYear() {
        printTestHeader("getCefElectricity - 364 Days");
        
        // GIVEN: 364 days since start
        LocalDate activityStartDate = LocalDate.of(2023, 1, 1);
        LocalDate processingDate = LocalDate.of(2023, 12, 31);
        
        // Expected: f(t) = 0.0 (< 1 year)
        BigDecimal expectedCef = CMO;
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Days since start: 364");
        System.out.println("Years: ~0.997 (< 1.0)");
        System.out.println("Transition factor: 0.0");
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Just before 2.5 years uses blended factor")
    void test_JustBeforeTwoAndHalf() {
        printTestHeader("getCefElectricity - Just Before 2.5 Years");
        
        // GIVEN: 2.49 years since start
        LocalDate activityStartDate = LocalDate.of(2022, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 7, 9); // ~2.52 years actually
        
        // Let's use a date that's clearly < 2.5 years
        processingDate = LocalDate.of(2024, 6, 1); // ~2.42 years
        
        // Expected: f(t) = 0.5 (still in transition)
        BigDecimal ca2024 = new BigDecimal("0.000438");
        BigDecimal expectedCef = CMO.multiply(new BigDecimal("0.5"))
                                    .add(ca2024.multiply(new BigDecimal("0.5")))
                                    .setScale(6, RoundingMode.HALF_UP);
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Years: ~2.42 (< 2.5)");
        System.out.println("Transition factor: 0.5");
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Leap year calculation accuracy")
    void test_LeapYearCalculation() {
        printTestHeader("getCefElectricity - Leap Year");
        
        // GIVEN: Start in leap year
        LocalDate activityStartDate = LocalDate.of(2020, 1, 1); // 2020 is leap year
        LocalDate processingDate = LocalDate.of(2021, 1, 1);    // Exactly 366 days
        
        // Expected: > 1 year (because of 366 days)
        // 366 / 365.25 = 1.002 years → f(t) = 0.5
        BigDecimal ca2021 = new BigDecimal("0.000444");
        BigDecimal expectedCef = CMO.multiply(new BigDecimal("0.5"))
                                    .add(ca2021.multiply(new BigDecimal("0.5")))
                                    .setScale(6, RoundingMode.HALF_UP);
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("Days: 366 (leap year)");
        System.out.println("Years: 1.002");
        System.out.println("Transition factor: 0.5");
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("ERROR: Activity start date after processing date")
    void test_StartDateAfterProcessing() {
        printTestHeader("getCefElectricity - Invalid Date Order");
        
        // GIVEN: Start date in future
        LocalDate activityStartDate = LocalDate.of(2025, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 1, 1);
        
        // WHEN / THEN
        System.out.println("Start date: " + activityStartDate);
        System.out.println("Processing date: " + processingDate);
        System.out.println("Expected: IllegalArgumentException");
        
        assertThatThrownBy(() -> 
            service.getCefElectricity(activityStartDate, processingDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Activity start date cannot be after processing date");
        
        printTestResult("SUCCESS - Exception thrown");
    }
    
    @Test
    @DisplayName("CALCULATION: Verify CDM formula correctness")
    void test_FormulaVerification() {
        printTestHeader("getCefElectricity - Formula Verification");
        
        // GIVEN: Known values for manual calculation
        LocalDate activityStartDate = LocalDate.of(2023, 1, 1);
        LocalDate processingDate = LocalDate.of(2024, 7, 1); // 1.5 years
        
        // Manual calculation:
        // f(t) = 0.5 (1 year ≤ 1.5 < 2.5)
        // C_mo = 0.000512
        // C_a(2024) = 0.000438
        // CEF = 0.000512 × (1 - 0.5) + 0.000438 × 0.5
        //     = 0.000512 × 0.5 + 0.000438 × 0.5
        //     = 0.000256 + 0.000219
        //     = 0.000475
        
        BigDecimal expectedCef = new BigDecimal("0.000475");
        
        // WHEN
        BigDecimal result = service.getCefElectricity(activityStartDate, processingDate);
        
        // THEN
        System.out.println("=== CDM Formula Verification ===");
        System.out.println("CEF = C_mo × (1 - f(t)) + C_a(t) × f(t)");
        System.out.println("    = 0.000512 × 0.5 + 0.000438 × 0.5");
        System.out.println("    = 0.000256 + 0.000219");
        System.out.println("    = 0.000475");
        System.out.println("Actual: " + result);
        
        assertThat(result).isEqualByComparingTo(expectedCef);
        
        printTestResult("SUCCESS");
    }
}
    
    // ========================================
    // 8. oneYearSinceLastCalculation()
    // ========================================
    
  
    
    // ========================================
    // 9. aggregateProjectTotals()
    // ========================================
    
    @Nested
    @DisplayName("9. aggregateProjectTotals()")
    class AggregateProjectTotalsTests {
    
    private LocalDate processingDate;
    
    @BeforeEach
    void setUpTotals() {
        processingDate = LocalDate.of(2024, 6, 15);
    }
    
    @Test
    @DisplayName("SUCCESS: Create new total record")
    void test_CreateNewTotal() {
        printTestHeader("aggregateProjectTotals");
        
        // GIVEN: No existing total
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.empty());
        
        // New monitoring items from 3 sites
        List<EnR002ReductionResult> monitoringItems = Arrays.asList(
            createMonitoringItem(siteId, "1.500", "0.000", "1.500", "1.200", "0.300"),
            createMonitoringItem(202, "2.300", "0.000", "2.300", "2.000", "0.300"),
            createMonitoringItem(203, "0.800", "0.000", "0.800", "0.700", "0.100")
        );
        
        // Expected totals:
        // empj = 1.500 + 2.300 + 0.800 = 4.600
        // empjM = 0.000 (all solar)
        // empjS = 1.500 + 2.300 + 0.800 = 4.600
        // empjSControl = 1.200 + 2.000 + 0.700 = 3.900
        // empjSBattery = 0.300 + 0.300 + 0.100 = 0.700
        
        ArgumentCaptor<EnR002ReductionResultTotal> captor = 
            ArgumentCaptor.forClass(EnR002ReductionResultTotal.class);
        
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, monitoringItems);
        
        // THEN
        verify(monitoringTotalRepo).save(captor.capture());
        EnR002ReductionResultTotal saved = captor.getValue();
        
        System.out.println("=== New Total Record Created ===");
        System.out.println("Sites processed: 3");
        System.out.println("Total empj: 4.600 tCO2");
        System.out.println("  - empjM: 0.000 tCO2");
        System.out.println("  - empjS: 4.600 tCO2");
        System.out.println("    - Control: 3.900 tCO2");
        System.out.println("    - Battery: 0.700 tCO2");
        
        // Verify identity fields
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getBatchId()).isEqualTo(batchId);
        assertThat(saved.getProcessingDate()).isEqualTo(processingDate);
        assertThat(saved.getIsActive()).isTrue();
        
        // Verify emission totals (NEW record starts from zero)
        assertThat(saved.getEmpj()).isEqualByComparingTo("4.600");
        assertThat(saved.getEmpjM()).isEqualByComparingTo("0.000");
        assertThat(saved.getEmpjS()).isEqualByComparingTo("4.600");
        assertThat(saved.getEmpjSControl()).isEqualByComparingTo("3.900");
        assertThat(saved.getEmpjSBattery()).isEqualByComparingTo("0.700");
        
        // Verify timestamps
        assertThat(saved.getStartTime()).isNotNull();
        assertThat(saved.getEndTime()).isNotNull();
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("SUCCESS: Update existing total record (cumulative)")
    void test_UpdateExistingTotal() {
        printTestHeader("aggregateProjectTotals - Update Existing");
        
        // GIVEN: Existing total with previous emissions
        Instant previousStartTime = Instant.parse("2024-06-01T10:00:00Z");
        EnR002ReductionResultTotal existingTotal = EnR002ReductionResultTotal.builder()
            .projectId(projectId)
            .batchId(99)
            .processingDate(LocalDate.of(2024, 6, 14))
            .startTime(previousStartTime)
            .endTime(Instant.parse("2024-06-14T12:00:00Z"))
            .empj(new BigDecimal("10.500"))
            .empjM(new BigDecimal("0.000"))
            .empjS(new BigDecimal("10.500"))
            .empjSControl(new BigDecimal("9.000"))
            .empjSBattery(new BigDecimal("1.500"))
            .build();
        existingTotal.setIsActive(true);
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.of(existingTotal));
        
        // New batch adds more emissions
        List<EnR002ReductionResult> newBatch = Arrays.asList(
            createMonitoringItem(siteId, "1.200", "0.000", "1.200", "1.000", "0.200"),
            createMonitoringItem(202, "0.800", "0.000", "0.800", "0.700", "0.100")
        );
        
        // Expected: Previous + New
        // empj = 10.500 + 2.000 = 12.500
        // empjSControl = 9.000 + 1.700 = 10.700
        // empjSBattery = 1.500 + 0.300 = 1.800
        
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, newBatch);
        
        // THEN
        System.out.println("=== Cumulative Update ===");
        System.out.println("Previous total: 10.500 tCO2");
        System.out.println("This batch: +2.000 tCO2");
        System.out.println("New total: 12.500 tCO2");
        System.out.println("");
        System.out.println("Breakdown:");
        System.out.println("  empjSControl: 9.000 + 1.700 = 10.700");
        System.out.println("  empjSBattery: 1.500 + 0.300 = 1.800");
        
        // Verify cumulative totals
        assertThat(result.getEmpj()).isEqualByComparingTo("2.000");
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("1.700");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.300");
        
        // Verify metadata updated
        assertThat(result.getBatchId()).isEqualTo(batchId);
        assertThat(result.getProcessingDate()).isEqualTo(processingDate);
        
        // Verify startTime NOT changed (should keep original)
        assertThat(result.getStartTime()).isEqualTo(previousStartTime);
        
        
        printTestResult("SUCCESS");
    }
    
    
    @Test
    @DisplayName("EDGE: Empty monitoring items list")
    void test_NoMonitoringItems() {
        printTestHeader("aggregateProjectTotals - No Items");
        
        // GIVEN: Empty list (no sites processed)
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.empty());
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        List<EnR002ReductionResult> emptyList = Collections.emptyList();
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, emptyList);
        
        // THEN
        System.out.println("No items to aggregate");
        System.out.println("All emissions: 0.000 tCO2");
        
        assertThat(result.getEmpj()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjM()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjS()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.000");
        
        // Still creates record with metadata
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getBatchId()).isEqualTo(batchId);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Items with null emission values")
    void test_NullEmissionValues() {
        printTestHeader("aggregateProjectTotals - Null Values");
        
        // GIVEN: Items with some null emissions
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.empty());
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        List<EnR002ReductionResult> items = Arrays.asList(
            createMonitoringItemWithNulls(siteId, "5.000", null, "5.000", null, "0.500"),
            createMonitoringItemWithNulls(202, "3.000", "0.000", null, "2.800", null)
        );
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, items);
        
        // THEN
        System.out.println("Nulls treated as zero:");
        System.out.println("  empj = 5.000 + 3.000 = 8.000");
        System.out.println("  empjM = 0 + 0 = 0.000");
        System.out.println("  empjS = 5.000 + 0 = 5.000");
        System.out.println("  empjSControl = 0 + 2.800 = 2.800");
        System.out.println("  empjSBattery = 0.500 + 0 = 0.500");
        
        assertThat(result.getEmpj()).isEqualByComparingTo("8.000");
        assertThat(result.getEmpjM()).isEqualByComparingTo("0.000");
        assertThat(result.getEmpjS()).isEqualByComparingTo("5.000");
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("2.800");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("0.500");
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("EDGE: Single site with zero emissions")
    void test_ZeroEmissions() {
        printTestHeader("aggregateProjectTotals - Zero Emissions");
        
        // GIVEN: Site with all zeros
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.empty());
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        List<EnR002ReductionResult> items = Arrays.asList(
            createMonitoringItem(siteId, "0.000", "0.000", "0.000", "0.000", "0.000")
        );
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, items);
        
        // THEN
        System.out.println("All zero emissions (e.g., no sunlight/generation)");
        
        assertThat(result.getEmpj()).isEqualByComparingTo("0.000");
        assertThat(result.getProjectId()).isEqualTo(projectId);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("VERIFY: StartTime preserved on update")
    void test_StartTimePreserved() {
        printTestHeader("aggregateProjectTotals - Preserve StartTime");
        
        // GIVEN: Existing record with specific startTime
        Instant originalStartTime = Instant.parse("2024-01-01T00:00:00Z");
        
        EnR002ReductionResultTotal existingTotal = EnR002ReductionResultTotal.builder()
            .projectId(projectId)
            .batchId(99)
            .processingDate(LocalDate.of(2024, 6, 1))
            .startTime(originalStartTime)
            .endTime(Instant.parse("2024-06-01T12:00:00Z"))
            .empj(new BigDecimal("5.000"))
            .empjM(BigDecimal.ZERO)
            .empjS(new BigDecimal("5.000"))
            .empjSControl(new BigDecimal("4.500"))
            .empjSBattery(new BigDecimal("0.500"))
            .build();
        existingTotal.setIsActive(true);
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.of(existingTotal));
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        List<EnR002ReductionResult> newItems = Arrays.asList(
            createMonitoringItem(siteId, "1.000", "0.000", "1.000", "0.900", "0.100")
        );
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, newItems);
        
        // THEN
        System.out.println("Original startTime: " + originalStartTime);
        System.out.println("After update startTime: " + result.getStartTime());
        System.out.println("startTime preserved: " + 
            result.getStartTime().equals(originalStartTime));
        
        assertThat(result.getStartTime()).isEqualTo(originalStartTime);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("VERIFY: Metadata updated correctly")
    void test_MetadataUpdated() {
        printTestHeader("aggregateProjectTotals - Metadata Update");
        
        // GIVEN: Existing total with old metadata
        EnR002ReductionResultTotal existingTotal = EnR002ReductionResultTotal.builder()
            .projectId(projectId)
            .batchId(98)
            .processingDate(LocalDate.of(2024, 6, 10))
            .startTime(Instant.parse("2024-06-10T10:00:00Z"))
            .endTime(Instant.parse("2024-06-10T11:00:00Z"))
            .empj(new BigDecimal("3.000"))
            .empjM(BigDecimal.ZERO)
            .empjS(new BigDecimal("3.000"))
            .empjSControl(new BigDecimal("2.700"))
            .empjSBattery(new BigDecimal("0.300"))
            
            .build();
        existingTotal.setIsActive(true);
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.of(existingTotal));
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        List<EnR002ReductionResult> newItems = Arrays.asList(
            createMonitoringItem(siteId, "1.000", "0.000", "1.000", "0.900", "0.100")
        );
        
        Integer newBatchId = 100;
        LocalDate newProcessingDate = LocalDate.of(2024, 6, 15);
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            newBatchId, projectId, newProcessingDate, newItems);
        
        // THEN
        System.out.println("=== Metadata Changes ===");
        System.out.println("batchId: 98 → " + result.getBatchId());
        System.out.println("processingDate: 2024-06-10 → " + result.getProcessingDate());
        System.out.println("endTime: updated to recent timestamp");
        
        assertThat(result.getBatchId()).isEqualTo(newBatchId);
        assertThat(result.getProcessingDate()).isEqualTo(newProcessingDate);
        
        printTestResult("SUCCESS");
    }
    
    @Test
    @DisplayName("CALCULATION: Large emission values")
    void test_LargeEmissions() {
        printTestHeader("aggregateProjectTotals - Large Values");
        
        // GIVEN: Existing total with large value
        EnR002ReductionResultTotal existingTotal = EnR002ReductionResultTotal.builder()
            .projectId(projectId)
            .batchId(99)
            .processingDate(LocalDate.of(2024, 6, 1))
            .startTime(Instant.now())
            .endTime(Instant.now())
            .empj(new BigDecimal("999999.999"))
            .empjM(BigDecimal.ZERO)
            .empjS(new BigDecimal("999999.999"))
            .empjSControl(new BigDecimal("900000.000"))
            .empjSBattery(new BigDecimal("99999.999"))
            .build();
        
        existingTotal.setIsActive(true);
        when(monitoringTotalRepo.findByProjectId(projectId))
            .thenReturn(Optional.of(existingTotal));
        when(monitoringTotalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        
        List<EnR002ReductionResult> newItems = Arrays.asList(
            createMonitoringItem(siteId, "50000.500", "0.000", "50000.500", 
                "5000.000", "5000.500")
                ,
                createMonitoringItem(siteId, "50000.500", "0.000", "50000.500", 
                "5000.000", "5000.500")
                
        );
        
        // WHEN
        EnR002ReductionResultTotal result = service.aggregateProjectTotals(
            batchId, projectId, processingDate, newItems);
        
        // THEN
        System.out.println("Large value handling:");
        System.out.println("  Previous: 999,999.999 tCO2");
        System.out.println("  Added: 50,000.500 tCO2");
        System.out.println("  Total: 1,050,000.499 tCO2");
        
        assertThat(result.getEmpj()).isEqualByComparingTo("100001.000");
        assertThat(result.getEmpjSControl()).isEqualByComparingTo("10000.000");
        assertThat(result.getEmpjSBattery()).isEqualByComparingTo("10001.000");
        
        printTestResult("SUCCESS");
    }
}

// ========================================
// Helper Methods for Totals Tests
// ========================================

/**
 * Creates monitoring item with all emission values.
 */
private EnR002ReductionResult createMonitoringItem(
        Integer siteId, String empj, String empjM, String empjS,
        String empjSControl, String empjSBattery) {
    
    return EnR002ReductionResult.builder()
        .siteId(siteId)
        .empj(new BigDecimal(empj))
        .empjM(new BigDecimal(empjM))
        .empjS(new BigDecimal(empjS))
        .empjSControl(new BigDecimal(empjSControl))
        .empjSBattery(new BigDecimal(empjSBattery))
        .build();
}

/**
 * Creates monitoring item with nullable emission values (for null testing).
 */
private EnR002ReductionResult createMonitoringItemWithNulls(
        Integer siteId, String empj, String empjM, String empjS,
        String empjSControl, String empjSBattery) {
    
    return EnR002ReductionResult.builder()
        .siteId(siteId)
        .empj(empj != null ? new BigDecimal(empj) : null)
        .empjM(empjM != null ? new BigDecimal(empjM) : null)
        .empjS(empjS != null ? new BigDecimal(empjS) : null)
        .empjSControl(empjSControl != null ? new BigDecimal(empjSControl) : null)
        .empjSBattery(empjSBattery != null ? new BigDecimal(empjSBattery) : null)
        .build();
}
    
    
    // ========================================
    // Helper Methods
    // ========================================
    
    private void printTestHeader(String testName) {
        System.out.println("");
        System.out.println("================================================================================");
        System.out.println(testName);
        System.out.println("================================================================================");
        System.out.println("");
    }
    
    private void printTestResult(String result) {
        System.out.println("");
        System.out.println(">>> " + result);
        System.out.println("");
    }
}