package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.SiteR002EquipmentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface SiteR002EquipmentFileRepository extends JpaRepository<SiteR002EquipmentFile, Integer> {

    List<SiteR002EquipmentFile> findByMemberData_Id(Integer memberDataId);

    @Query(value = "SELECT f.original_name FROM site_r002_equipment_file f "
         + "WHERE f.member_data_id = :memberDataId "
         + "AND f.deleted_at IS NULL "
         + "AND f.original_name IS NOT NULL "
         + "AND BTRIM(f.original_name) <> '' "
         + "ORDER BY f.id",
         nativeQuery = true)
    List<String> findOriginalNamesByMemberDataId(@Param("memberDataId") Integer memberDataId);

    @Modifying
    @Query("UPDATE SiteR002EquipmentFile f SET f.s3Key = :s3Key WHERE f.id = :id")
    void updates3Key(@Param("id") Integer id, @Param("s3Key") String s3Key);

    @Modifying
    @Query(value = "UPDATE site_r002_equipment_file f SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy "
         + "WHERE f.member_data_id IN (SELECT md.id FROM site_r002_member_data md "
         + "JOIN participating_company pc ON pc.id = md.participating_company_id "
         + "WHERE pc.company_id = :companyId)",
         nativeQuery = true)
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}
