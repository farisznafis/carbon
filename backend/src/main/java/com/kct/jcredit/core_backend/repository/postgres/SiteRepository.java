package com.kct.jcredit.core_backend.repository.postgres;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    public List<Site> findByCompany(ParticipatingCompany company);
    public List<Site> findByCompanyIdAndPicEmail(Integer companyId, String picEmail);
}
