package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;

import jakarta.transaction.Transactional;

import com.kct.jcredit.core_backend.entity.ParticipatingProject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

public interface ParticipatingProjectRepository
        extends JpaRepository<ParticipatingProject, Integer> {
    @Modifying
    @Transactional
    @Query("UPDATE ParticipatingProject p SET p.s3KeyPdfMembership = :s3Key WHERE p.id = :id")
    void updatePdfS3Key(@Param("id") Integer id, @Param("s3Key") String s3Key);

    Optional<ParticipatingProject> findByEquipment_Site_IdAndProject_Id(Integer siteId, Integer projectId);

    List<ParticipatingProject> findByProject_Id(Integer projectId);
    
    Optional<ParticipatingProject> findByIdAndProject_Id(Integer participationId, Integer projectId);

    Optional<ParticipatingProject> findByCompanyIdAndProject_Id(Integer participatingCompanyId, Integer projectId);

    List<ParticipatingProject> findByEquipment_Id(Integer equipmentId);

    List<ParticipatingProject> findByProject_IdAndEquipment_Site_Company_CompanyId(
        Integer projectId, String companyId);
        
    List<ParticipatingProject> findByStatus(String status);
        
    Optional<ParticipatingProject> findByDocusignEnvelopeId(String envelopeId);

}