package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingProject;

import jakarta.transaction.Transactional;

import com.kct.jcredit.core_backend.entity.ParticipatingProject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

public interface ParticipatingProjectRepository extends JpaRepository<ParticipatingProject, Integer> {

    Optional<ParticipatingProject> findBySiteProjectRelation_Id(Integer siteProjectRelationId);

    // Cek apakah site sudah pernah participation di project tertentu
    boolean existsBySiteProjectRelation_Site_IdAndSiteProjectRelation_Project_Id(
            Integer siteId, Integer projectId);

    // Ambil participation by company — untuk list di dashboard
    List<ParticipatingProject> findByCompany_CompanyId(String companyId);

    @Modifying
    @Transactional
    @Query("UPDATE ParticipatingProject p SET p.s3KeyPdfMembership = :s3Key WHERE p.id = :id")
    void updatePdfS3Key(@Param("id") Integer id, @Param("s3Key") String s3Key);
}
