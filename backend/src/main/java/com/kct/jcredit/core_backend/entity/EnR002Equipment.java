package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "en_r002_equipment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnR002Equipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "equipment_op_date", nullable = false)
    private LocalDate equipmentOpDate;

    @Column(name = "nominal_max_output", precision = 10, scale = 1, nullable = false)
    private BigDecimal nominalMaxOutput;

    @Column(name = "pcs_manufacturer", length = 100, nullable = false)
    private String pcsManufacturer;

    @Column(name = "pcs_model", length = 50, nullable = false)
    private String pcsModel;

    @Column(name = "pcs_unit_count")
    private Integer pcsUnitCount;

    @Column(name = "device_no", length = 100, nullable = false)
    private String deviceNo;

    @Column(name = "pcs_standby", precision = 8, scale = 2, nullable = false)
    private BigDecimal pcsStandby;

    @Column(name = "has_battery", nullable = false)
    private Boolean hasBattery;

    @Column(name = "battery_maker", length = 100)
    private String batteryMaker;

    @Column(name = "battery_model", length = 100)
    private String batteryModel;

    @Column(name = "battery_capacity", precision = 10, scale = 1)
    private BigDecimal batteryCapacity;

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

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnR002EquipmentFile> files = new ArrayList<>();
}