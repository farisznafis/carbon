package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnR002EquipmentRepository extends JpaRepository<EnR002Equipment, Integer> {

    // List equipment site
    List<EnR002Equipment> findBySite_Id(Integer siteId);

    boolean existsBySite_Id(Integer siteId);
}
