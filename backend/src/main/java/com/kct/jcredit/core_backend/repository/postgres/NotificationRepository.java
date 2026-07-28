package com.kct.jcredit.core_backend.repository.postgres;


import com.kct.jcredit.core_backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 通知リポジトリ
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
     
    //Participating User - 未読フィルタあり
    @Query("""
        SELECT n FROM Notification n
        WHERE n.participatingUser.id = :userId
        AND n.read = :read
        AND n.isActive = true
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findByParticipatingUserIdAndReadAndIsActive(
        @Param("userId") Integer userId,
        @Param("read") Boolean read,
        Pageable pageable
    );
    
    // Participating User - 全件
    @Query("""
        SELECT n FROM Notification n
        WHERE n.participatingUser.id = :userId
        AND n.isActive = true
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findByParticipatingUserIdAndIsActive(
        @Param("userId") Integer userId,
        Pageable pageable
    );
    
    // Operating User - 未読フィルタあり
    @Query("""
        SELECT n FROM Notification n
        WHERE n.operatingUser.id = :userId
        AND n.read = :read
        AND n.isActive = true
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findByOperatingUserIdAndReadAndIsActive(
        @Param("userId") Integer userId,
        @Param("read") Boolean read,
        Pageable pageable
    );
    
    // Operating User - 全件
    @Query("""
        SELECT n FROM Notification n
        WHERE n.operatingUser.id = :userId
        AND n.isActive = true
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findByOperatingUserIdAndIsActive(
        @Param("userId") Integer userId,
        Pageable pageable
    );
    
    // 一括既読
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids")
    void markNotificationsAsRead(@Param("ids") List<Integer> ids);
    
    // 未読数カウント（Participating User）
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.participatingUser.id = :userId
        AND n.read = false
        AND n.isActive = true
    """)
    long countUnreadByParticipatingUserId(@Param("userId") Integer userId);
    // 未読数カウント（Operating User）
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.operatingUser.id = :userId
        AND n.read = false
        AND n.isActive = true
    """)
    long countUnreadByOperatingUserId(@Param("userId") Integer userId);
        
}