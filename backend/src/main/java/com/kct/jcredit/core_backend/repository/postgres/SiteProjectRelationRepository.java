package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;


public interface SiteProjectRelationRepository extends JpaRepository<SiteProjectRelation, Integer> {
    boolean existsByProjectAndSite(Project project, Site site);

    List<SiteProjectRelation> findBySite(Site site);

    List<SiteProjectRelation> findByProject(Project project);

    List<SiteProjectRelation> findByProjectId(Integer projectId);

    Optional<SiteProjectRelation> findBySiteIdAndProjectId(Integer siteId, Integer projectId);
    
    SiteProjectRelation findByProjectIdAndSiteId(Integer projectId, Integer siteId);

    @Query("SELECT spr.site.id FROM SiteProjectRelation spr WHERE spr.project.id = :projectId")
    List<Integer> findSiteIdsByProjectId(@Param("projectId") Integer projectId);

    @Modifying
    @Query(value = "UPDATE site_project_relation spr SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy "
         + "WHERE spr.site_id IN (SELECT s.id FROM site s JOIN participating_company pc ON pc.id = s.participating_company_id WHERE pc.company_id = :companyId)", nativeQuery = true)
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}