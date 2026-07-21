package com.kct.jcredit.core_backend.repository.postgres;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    public List<Site> findByCompany(ParticipatingCompany company);

    @Query("""
            SELECT DISTINCT site
            FROM Site site
            LEFT JOIN FETCH site.siteProjectRelations relation
            LEFT JOIN FETCH relation.project
            WHERE site.company.id = :companyId
            """)
    List<Site> findDetailsByCompanyId(@Param("companyId") Integer companyId);

    public List<Site> findByCompanyIdAndPicEmail(Integer companyId, String picEmail);
    long countByCompany_Id(Integer companyId);

    @Modifying
    @Query("UPDATE Site s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.company.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);

    Optional<Site> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);
}
