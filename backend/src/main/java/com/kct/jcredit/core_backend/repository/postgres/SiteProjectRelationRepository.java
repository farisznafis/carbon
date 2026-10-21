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

    Optional<SiteProjectRelation> findBySite_IdAndProject_Id(
            Integer siteId, Integer projectId);

    boolean existsBySite_IdAndProject_Id(
            Integer siteId, Integer projectId);

        boolean existsBySite_Company_CompanyIdAndProject_Id(String companyId, Integer projectId);

        boolean existsBySite_Id(Integer siteId); // ← tambah ini

    List<SiteProjectRelation> findBySite_Id(Integer siteId);

    List<SiteProjectRelation> findByProject_Id(Integer projectId);

    List<SiteProjectRelation> findBySite(Site site);

    List<SiteProjectRelation> findByProject(Project project);

    List<SiteProjectRelation> findByProjectId(Integer projectId);

    Optional<SiteProjectRelation> findBySiteIdAndProjectId(Integer siteId, Integer projectId);
    
    SiteProjectRelation findByProjectIdAndSiteId(Integer projectId, Integer siteId);

    @Query("SELECT spr.site.id FROM SiteProjectRelation spr WHERE spr.project.id = :projectId")
    List<Integer> findSiteIdsByProjectId(@Param("projectId") Integer projectId);

    /** A site that is, or once was, linked to a project. */
    interface LinkedSite {
        Integer getSiteId();
        String getSitePublicId();
    }

    /**
     * Every site ever linked to the project, including soft-deleted relations and sites.
     * Native so it bypasses the deleted_at restrictions: monitoring records written before
     * a site left the project are still stored under that site's key.
     */
    @Query(value = "SELECT DISTINCT s.id AS siteId, s.public_id AS sitePublicId "
         + "FROM site_project_relation spr JOIN site s ON s.id = spr.site_id "
         + "WHERE spr.project_id = :projectId ORDER BY s.id", nativeQuery = true)
    List<LinkedSite> findLinkedSitesIncludingDeleted(@Param("projectId") Integer projectId);

    @Modifying
    @Query(value = "UPDATE site_project_relation spr SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy "
         + "WHERE spr.site_id IN (SELECT s.id FROM site s JOIN participating_company pc ON pc.id = s.participating_company_id WHERE pc.company_id = :companyId)", nativeQuery = true)
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);

    
}