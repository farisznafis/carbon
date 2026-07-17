package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.SecurityActionLink;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityActionLinkRepository extends JpaRepository<SecurityActionLink, UUID> {

    Optional<SecurityActionLink> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SecurityActionLink l where l.id = :id")
    Optional<SecurityActionLink> findByIdForUpdate(@Param("id") UUID id);

    List<SecurityActionLink> findBySubjectTypeAndSubjectIdAndPurposeAndStatusIn(
            SecurityActionSubjectType subjectType,
            Integer subjectId,
            SecurityActionPurpose purpose,
            List<SecurityActionStatus> statuses);

    @Modifying
    @Query("delete from SecurityActionLink l where l.status in :statuses and " +
            "coalesce(l.consumedAt, l.revokedAt, l.expiresAt) < :cutoff")
    int deleteTerminalBefore(
            @Param("statuses") List<SecurityActionStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("update SecurityActionLink l set l.status = :expiredStatus " +
            "where l.status in :activeStatuses and l.expiresAt < :now")
    int expireActiveBefore(
            @Param("activeStatuses") List<SecurityActionStatus> activeStatuses,
            @Param("expiredStatus") SecurityActionStatus expiredStatus,
            @Param("now") LocalDateTime now);
}
