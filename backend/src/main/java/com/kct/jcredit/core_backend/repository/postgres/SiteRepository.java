package com.kct.jcredit.core_backend.repository.postgres;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    public List<Site> findByCompany(ParticipatingCompany company);
    public List<Site> findByCompanyIdAndPicEmail(Integer companyId, String picEmail);

    @Modifying
    @Query("UPDATE Site s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.company.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}
