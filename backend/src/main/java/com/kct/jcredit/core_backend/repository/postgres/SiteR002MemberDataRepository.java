package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteR002MemberDataRepository extends JpaRepository<SiteR002MemberData, Integer> {

    List<SiteR002MemberData> findBySite_IdAndProject_Id(Integer siteId, Integer projectId);

    List<SiteR002MemberData> findByCompany_CompanyIdIn(List<String> companyIds);

    Optional<SiteR002MemberData> findByProject_IdAndSite_Id(Integer projectId, Integer siteId);

    boolean existsBySite_IdAndProject_Id(Integer siteId, Integer projectId);

    void deleteByCompany_CompanyId(String companyId);
}