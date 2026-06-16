package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity{

    @Column(name = "acct_no", nullable = false, unique = true, length = 20)
    private String acctNo;

    @Column(name = "holder_account_name_kana", nullable = false, length = 50)
    private String holderAccountNameKana;

    @Column(name = "holder_account_name", nullable = false, length = 50)
    private String holderAccountName;

    @Column(name = "holder_corp_number", unique = true, length = 13)
    private String holderCorpNumber;

    @Column(name = "holder_prefecture", nullable = false, length = 100)
    private String holderPrefecture;

    @Column(name = "holder_city", nullable = false, length = 100)
    private String holderCity;

    @Column(name = "holder_address", nullable = false, length = 500)
    private String holderAddress;

    @Column(name = "is_holder_undecided", nullable = false)
    private Boolean isHolderUndecided;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_company_id", nullable = false)
    private OperatorCompany operatorCompany;
}
