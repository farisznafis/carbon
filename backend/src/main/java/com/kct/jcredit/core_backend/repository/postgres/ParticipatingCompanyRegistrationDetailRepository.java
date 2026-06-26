package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingCompanyRegistrationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipatingCompanyRegistrationDetailRepository
        extends JpaRepository<ParticipatingCompanyRegistrationDetail, Integer> {

    Optional<ParticipatingCompanyRegistrationDetail> findByCompany(ParticipatingCompany company);

    List<ParticipatingCompanyRegistrationDetail> findByCompanyIn(Collection<ParticipatingCompany> companies);
}
