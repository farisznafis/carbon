package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.converter.StringListJsonConverter;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(
        name = "participating_company_registration_detail",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_participating_company_registration_detail_company",
                        columnNames = "participating_company_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipatingCompanyRegistrationDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participating_company_id", nullable = false)
    private ParticipatingCompany company;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "requested_methodologies", nullable = false, columnDefinition = "TEXT")
    private List<String> requestedMethodologies;

    @Column(name = "activity_information", columnDefinition = "TEXT")
    private String activityInformation;
}
