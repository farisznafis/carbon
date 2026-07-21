# Batch Processing Flow Diagram

## Entry Point

**Method to Initiate the Batch Process:**
```java
executeAllProjects(LocalDate processingDate)
```

This is the main entry point that starts the entire batch processing pipeline.

---

## Complete Processing Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        START: executeAllProjects()                      │
│                      (Initiates batch processing)                       │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │ checkForRunningBatch() │
                    │  (Verify no conflict)  │
                    └────────────┬───────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │createBatchHistory()    │
                    │ (Create batch record)  │
                    │  Returns: batchId      │
                    └────────────┬───────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │  getActiveProjects()   │
                    │  Returns: List<Integer>│
                    │    (Project IDs)       │
                    └────────────┬───────────┘
                                 │
                                 ▼
               ┌──────────────────────────────────────┐
               │   processAllProjects(batchId,        │
               │                    projectIds,       │
               │                    processingDate)   │
               └──────────────┬───────────────────────┘
                              │
                    ╔═════════╩═════════╗
                    ║  FOR EACH PROJECT ║
                    ╚═════════╦═════════╝
                              │
                              ▼
               ┌──────────────────────────────────────┐
               │   processProject(batchId,            │
               │               projectId,             │
               │               processingDate, ...)   │
               └──────────────┬───────────────────────┘
                              │
                              ▼
                ┌─────────────────────────────┐
                │  getSitesForProject()       │
                │  Returns: List<Integer>     │
                │    (Site IDs for project)   │
                └──────────────┬──────────────┘
                               │
                               ▼
            ┌────────────────────────────────────┐
            │getMonitoringRepository()           │
            │.findUnprocessedRecordsForProject() │
            │  Returns: Map<Integer, List<T>>    │
            │  (Site ID → Monitoring Records)    │
            └──────────────┬─────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  Create CSV paths and  │
              │  Target/Result logs    │
              └──────────────┬─────────┘
                             │
                   ╔═════════╩═════════╗
                   ║   FOR EACH SITE   ║
                   ╚═════════╦═════════╝
                             │
                             ▼
              ┌──────────────────────────────────┐
              │    processSite(batchId,          │
              │              projectId,          │
              │              siteId,             │
              │              siteData,           │
              │              processingDate,     │
              │              allResults)         │
              └────────────┬─────────────────────┘
                           │
                           ▼
              ┌────────────────────────────────────┐
              │   getCalculator()                  │
              │.calculateEmissions(projectId,      │
              │                    siteId,         │
              │                    batchId,        │
              │                    siteData,       │
              │                    processingDate) │ 
              │  Returns: R (Result Entity)        │
              └────────────┬───────────────────────┘
                           │
                           ▼
              ┌────────────────────────────────────┐
              │   saveSiteResult(...)              │
              │  (Persist site-level result)       │
              │                                    │
              │  For Solar:                        │
              │  EnR002ReductionResult → Database  │
              └────────────┬───────────────────────┘
                           │
                           ▼
              ┌────────────────────────────────────┐
              │  allResults.add(result)            │
              │  (Collect all site results)        │
              └────────────┬───────────────────────┘
                           │
                  ╚════════╩════════╝
                  (Loop ends, all
                   sites processed)
                           │
                           ▼
              ┌────────────────────────────────────┐
              │ Compare CSV Logs                   │
              │ (Verify no duplicate processing)   │
              └────────────┬───────────────────────┘
                           │
                           ▼
              ┌────────────────────────────────────┐
              │  aggregateEmissions(allResults)    │
              │  Returns: Object (aggregated data) │
              │                                    │
              │  For Solar:                        │
              │  Sum all site results into         │
              │  EnR002ReductionResultTotal        │
              └────────────┬───────────────────────┘
                           │
                           ▼
              ┌────────────────────────────────────┐
              │  saveProjectTotal(projectId,       │
              │                   batchId,         │
              │                   processingDate,  │
              │                   siteResults,     │
              │                   aggregated)      │
              │                                    │
              │  (Persist project-level totals)    │
              │                                    │
              │  For Solar:                        │
              │  EnR002ReductionResultTotal →      │
              │  Database (create or update)       │
              └────────────┬───────────────────────┘
                           │
                  ╚════════╩════════╝
                  (Loop ends, all
                   projects processed)
                           │
                           ▼
              ┌────────────────────────────────────┐
              │ updateBatchHistoryOnCompletion()   │
              │ (Mark batch as COMPLETED/FAILED)   │
              └────────────┬───────────────────────┘
                           │
                           ▼
                ┌──────────────────────────┐
                │    END: Batch Complete   │
                └──────────────────────────┘
```

---

## Method Call Sequence Summary

| Step | Method | Parameters | Returns | Purpose |
|------|--------|-----------|---------|---------|
| 1 | `executeAllProjects()` | `LocalDate processingDate` | `void` | **Entry point** - Initiates entire batch |
| 2 | `checkForRunningBatch()` | - | `void` | Verify no other batch is running |
| 3 | `createBatchHistory()` | - | `BatchProcessHistory` | Create batch record, get `batchId` |
| 4 | `getActiveProjects()` | - | `List<Integer>` | Get all project IDs to process |
| 5 | `processAllProjects()` | `batchId, projectIds, processingDate` | `BatchResult` | Process each project in loop |
| 6 | `processProject()` | `batchId, projectId, processingDate, ...` | `void` | Process single project |
| 7 | `getSitesForProject()` | `projectId` | `List<Integer>` | Get all sites in project |
| 8 | `getMonitoringRepository().findUnprocessedRecordsForProject()` | `projectId, siteIds, ...` | `Map<Integer, List<T>>` | Load monitoring data from DynamoDB |
| 9 | `processSite()` | `batchId, projectId, siteId, siteData, processingDate, allResults` | `ProcessingResult` | Process single site (loop) |
| 10 | `getCalculator().calculateEmissions()` | `projectId, siteId, batchId, siteData, processingDate` | `R` | Calculate emissions for site |
| 11 | `saveSiteResult()` | `projectId, siteId, batchId, processingDate, result` | `Object` | **Save site-level result** |
| 12 | `aggregateEmissions()` | `List<R> allResults` | `Object` | Sum all site results |
| 13 | `saveProjectTotal()` | `projectId, batchId, processingDate, siteResults, aggregated` | `Object` | **Save project-level total** |
| 14 | `updateBatchHistoryOnCompletion()` | `batchHistory, result` | `void` | Mark batch complete |

---

## Key Entry Point Code

To start the batch process programmatically:

```java
@Autowired
private SolarBatchProcessService solarBatchProcessService;

// Start batch processing
LocalDate processingDate = LocalDate.now();
solarBatchProcessService.executeAllProjects(processingDate);
```

Or for a specific date:

```java
LocalDate processingDate = LocalDate.of(2026, 6, 4);
solarBatchProcessService.executeAllProjects(processingDate);
```

---

## Data Flow Highlights

### Input
- **processingDate**: The date for which to process emissions

### Processing Path
1. **Load**: Monitoring records from DynamoDB per site
2. **Calculate**: Emissions for each site using methodology-specific calculator
3. **Save Site Results**: Persist individual site calculation results
4. **Aggregate**: Sum all site results into project totals
5. **Save Project Total**: Persist aggregated project-level totals

### Output
- **Site-level**: Individual `EnR002ReductionResult` entities (for solar methodology)
- **Project-level**: Aggregated `EnR002ReductionResultTotal` entities
- **Batch History**: Tracking record with COMPLETED/FAILED status

---

## Exception Handling

- **Running batch check**: Prevents concurrent batch execution
- **CSV comparison**: Detects duplicate processing
- **Per-site error handling**: Individual site failures are logged; batch continues
- **All-project failure**: If all projects fail, batch is marked FAILED

---

## For Solar Methodology Specifically

- **Calculator**: `SolarEmissionsCalculator` returns `EnR002ReductionResult`
- **Site persistence**: `SolarBatchProcessService.saveSiteResult()` → `EnR002ReductionResultRepository`
- **Aggregation**: `SolarBatchProcessService.aggregateEmissions()` → Sums all site results
- **Project persistence**: `SolarBatchProcessService.saveProjectTotal()` → `EnR002ReductionResultTotalRepository`

