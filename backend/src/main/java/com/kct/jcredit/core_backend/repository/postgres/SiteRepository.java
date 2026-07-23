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
    List<Site> findByCompany_CompanyId(String companyId);
    long countByCompany_Id(Integer companyId);

    @Modifying
    @Query("UPDATE Site s SET s.deletedAt = CURRENT_TIMESTAMP, s.deletedBy = :deletedBy WHERE s.company.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);

    Optional<Site> findByPublicId(String publicId);
    boolean existsByPublicId(String publicId);

    default Optional<Site> findByPublicIdWithFallback(String publicId) {
        try {
            Optional<Site> site = findByPublicId(publicId);
            if (site.isPresent()) {
                return site;
            }
        } catch (Exception e) {
            // ignore exception
        }
        
        if (publicId != null && publicId.startsWith("SITE")) {
            try {
                Integer id = Integer.parseInt(publicId.substring(4));
                return findById(id);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        if (publicId != null && publicId.matches("\\d+")) {
            try {
                return findById(Integer.parseInt(publicId));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return Optional.empty();
    }
}
