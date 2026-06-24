package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "site_r002_member_data")
@SQLDelete(sql = "UPDATE site_r002_member_data SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SiteR002MemberData extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, unique = true)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "company_id", nullable = false) 
    @JsonIgnore
    private ParticipatingCompany company;

    @Column(name = "methodology_number", length = 3, unique = true, columnDefinition = "CHAR(3)")
    private String methodologyNumber;

    @Column(name = "is_eligible", nullable = false)
    private Boolean isEligible;

    @Column(name = "equipment_op_date", nullable = false)
    private LocalDate equipmentOpDate;

    @Column(name = "nominal_max_output", precision = 10, scale = 1, nullable = false)
    private BigDecimal nominalMaxOutput;

    @Column(name = "pcs_manufacturer", length = 100, nullable = false)
    private String pcsManufacturer;

    @Column(name = "pcs_model", length = 50, nullable = false)
    private String pcsModel;

    @Column(name = "power_cond_unit_count", nullable = false)
    private Integer powerCondUnitCount;

    @Column(name = "device_no", length = 100, nullable = false)
    private String deviceNo;

    @Column(name = "power_cond_standby", precision = 8, scale = 2, nullable = false)
    private BigDecimal powerCondStandby;

    @Column(name = "has_battery", nullable = false)
    private Boolean hasBattery;

    @Column(name = "battery_maker", length = 100)
    private String batteryMaker;

    @Column(name = "battery_model", length = 100)
    private String batteryModel;

    @Column(name = "battery_capacity", precision = 10, scale = 1)
    private BigDecimal batteryCapacity;

    @Column(name = "ict_equipment_power_consumption", precision = 12, scale = 3, nullable = false)
    private BigDecimal ictEquipmentPowerConsumption;

    @Column(name = "additional_equipment")
    private Boolean additionalEquipment;

    @Column(name = "additional_equipment_date")
    private LocalDate additionalEquipmentDate;

    @Column(name = "equipment_type", length = 50)
    private String additionalEquipmentType;

    @Column(name = "unit_count")
    private Integer additionalUnitCount;

    @Column(name = "device_unique_no", length = 100)
    private String additionalDeviceUniqueNo;

    @Column(name = "total_investment")
    private Integer totalInvestment;

    @Column(name = "has_subsidy", nullable = false)
    private Boolean hasSubsidy;

    @Column(name = "subsidy_name", length = 200)
    private String subsidyName;

    @Column(name = "subsidy_amount", precision = 15, scale = 0)
    private BigDecimal subsidyAmount;

    @Column(name = "pre_project_unit_price", precision = 10, scale = 2)
    private BigDecimal preProjectUnitPrice;

    @OneToMany(mappedBy = "memberData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SiteR002EquipmentFile> files = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}