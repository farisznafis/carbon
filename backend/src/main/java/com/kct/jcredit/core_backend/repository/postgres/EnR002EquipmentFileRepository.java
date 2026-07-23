package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface EnR002EquipmentFileRepository extends JpaRepository<EnR002EquipmentFile, Integer> {

    // List<EnR002EquipmentFile> findByMemberData_Id(Integer memberDataId);

    List<EnR002EquipmentFile> findByEquipment_Id(Integer equipmentId);

    @Modifying
    @Transactional
    @Query("UPDATE EnR002EquipmentFile f SET f.s3Key = :s3Key WHERE f.id = :id")
    void updateS3Key(@Param("id") Integer id, @Param("s3Key") String s3Key);

    @Modifying
    @Transactional
    @Query("UPDATE EnR002EquipmentFile f SET f.s3Key = :s3Key WHERE f.id = :id")
    void updates3Key(@Param("id") Integer id, @Param("s3Key") String s3Key);

    @Modifying
    @Transactional
    @Query(value = "UPDATE en_r002_equipment_file f SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy "
         + "WHERE f.equipment_id IN (SELECT md.id FROM en_r002_equipment md WHERE md.company_id = :companyId)",
         nativeQuery = true)
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}