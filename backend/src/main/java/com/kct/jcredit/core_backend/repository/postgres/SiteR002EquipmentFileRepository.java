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

    @Modifying
    @Query("UPDATE SiteR002EquipmentFile f SET f.s3Key = :s3Key WHERE f.id = :id")
    void updates3Key(@Param("id") Integer id, @Param("s3Key") String s3Key);
}