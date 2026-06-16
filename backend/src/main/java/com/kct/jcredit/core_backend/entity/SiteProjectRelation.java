package com.kct.jcredit.core_backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "site_project_relation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "site_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteProjectRelation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "activity_start_date", nullable = false)
    private LocalDate activityStartDate;

    @Column(name = "status", nullable = false)
    private String status;
}
