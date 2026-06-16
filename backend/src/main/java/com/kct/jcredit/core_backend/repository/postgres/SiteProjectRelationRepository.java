package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public interface SiteProjectRelationRepository extends JpaRepository<SiteProjectRelation, Integer> {
    boolean existsByProjectAndSite(Project project, Site site);

    List<SiteProjectRelation> findBySite(Site site);

    List<SiteProjectRelation> findByProject(Project project);

    List<SiteProjectRelation> findByProjectId(Integer projectId);

    Optional<SiteProjectRelation> findBySiteIdAndProjectId(Integer siteId, Integer projectId);
    
    SiteProjectRelation findByProjectIdAndSiteId(Integer projectId, Integer siteId);
}