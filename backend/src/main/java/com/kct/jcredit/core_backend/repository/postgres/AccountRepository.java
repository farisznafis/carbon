package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    /**
     * Active Ｊ－クレジット accounts of one operator company.
     *
     * <p>Returns a list rather than an {@code Optional}: {@code account} has a
     * plain {@code operator_company_id} foreign key with no unique constraint,
     * so a company may hold several. Callers decide what to do when it does.
     */
    List<Account> findByOperatorCompany_IdAndIsActiveTrue(Integer operatorCompanyId);
}
