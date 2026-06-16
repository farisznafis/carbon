package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Data;
import java.util.List;

@Data
public class ProjectDetailMonitoringResponse {

    private ProjectHeader header;
    private ReductionChart chart;
    private TotalReductionWidget totalWidget;
    private ProjectTimeline timeline;
    private List<DocumentInfo> documents;
    private List<SiteInfo> sites;
    private List<EvidenceDocument> evidenceDocuments;

    @Data
    public static class ProjectHeader {
        private String projectId;
        private String projectName;
        private String applicant;
        private String methodology;
        private String projectType;
        private String registrationNumber;
        private ProjectPeriod projectPeriod;
        private String approvalMethod;
        private Integer participantCount;
        private String status;
        private String creditStatus;
    }

    @Data
    public static class ProjectPeriod {
        private String startDate;
        private String endDate;
        private Integer totalMonths;
    }

    @Data
    public static class ReductionChart {
        private List<ChartData> chartData;
        private TooltipInfo tooltipInfo;
    }

    @Data
    public static class ChartData {
        private String year;
        private Integer baseline;
        private Integer actualEmission;
        private Integer reduction;
    }

    @Data
    public static class TooltipInfo {
        private String lastUpdated;
        private String source;
        private String note;
        private Boolean adjustmentApplied;
        private String carbonCredits;
    }

    @Data
    public static class TotalReductionWidget {
        private ReductionValue totalReduction;
        private ClickThrough clickThrough;
    }

    @Data
    public static class ReductionValue {
        private Integer value;
        private String unit;
    }

    @Data
    public static class ClickThrough {
        private Integer initialEstimate;
        private Integer currentTotal;
        private Integer increase;
        private String unit;
    }

    @Data
    public static class ProjectTimeline {
        private Integer startYear;
        private Integer endYear;
        private Integer currentYear;
        private List<Milestone> milestones;
    }

    @Data
    public static class Milestone {
        private Integer year;
        private String status;
        private String label;
    }

    @Data
    public static class DocumentInfo {
        private Integer id;
        private String fileName;
        private String uploadDate;
        private String fileSize;
        private String type;
        private String downloadUrl;
    }

    @Data
    public static class SiteInfo {
        private String siteId;
        private String siteName;
        private List<PowerConditioner> powerConditioners;
        private SolarPanels solarPanels;
        private IctEquipment ictEquipment;
        private InstallationCosts installationCosts;
        private FundingSources fundingSources;
        private String equipmentType;
    }

    @Data
    public static class PowerConditioner {
        private String id;
        private String maker;
        private String model;
        private String capacity;
        private String installationDate;
        private String registrationNumber;
    }

    @Data
    public static class SolarPanels {
        private String totalCapacity;
        private String panelCount;
        private String maker;
    }

    @Data
    public static class IctEquipment {
        private String maker;
        private String model;
        private String communicationMethod;
    }

    @Data
    public static class InstallationCosts {
        private String total;
        private List<String> breakdown;
    }

    @Data
    public static class FundingSources {
        private String subsidy;
        private String own;
    }

    @Data
    public static class EvidenceDocument {
        private Integer id;
        private String fileName;
        private String type;
        private String uploadDate;
        private String downloadUrl;
    }
}