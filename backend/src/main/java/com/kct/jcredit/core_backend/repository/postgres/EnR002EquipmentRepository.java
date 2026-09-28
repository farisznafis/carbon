package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EnR002EquipmentRepository extends JpaRepository<EnR002Equipment, Integer> {

    // List equipment site
    List<EnR002Equipment> findBySite_Id(Integer siteId);

    boolean existsBySite_Id(Integer siteId);

    Optional<EnR002Equipment> findByPreviousEquipment_Id(Integer previousEquipmentId);

    Optional<EnR002Equipment> findByPreviousEquipmentId(Integer equipmentId);

    @Query("""
        SELECT MIN(e.equipmentOpDate) FROM EnR002Equipment e
        WHERE e.id IN (SELECT pp.equipment.id FROM ParticipatingProject pp WHERE pp.project.id = :projectId)
        """)
    LocalDate findEarliestOperationDateByProjectId(@Param("projectId") Integer projectId);
}
