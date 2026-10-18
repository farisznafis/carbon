package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 通知エンティティ
 * 
 * アプリ内通知を管理するテーブル
 */
@Entity
@Table(name = "notification")
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {
    
    /**
     * 通知タイトル
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;
    
    /**
     * 通知メッセージ内容（最大1000文字）
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    /**
     * 既読フラグ
     */
    @Column(name = "read", nullable = false)
    @Builder.Default
    private Boolean read = false;
    
    /**
     * 運営ユーザーID（操作を実行したユーザー）
     * システム通知の場合はNULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operating_user_id", 
                foreignKey = @ForeignKey(name = "fk_notification_operating_user"))
    private OperatingUser operatingUser;
    
    /**
     * 参加企業ユーザーID（通知を受け取るユーザー）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participating_user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notification_participating_user"))
    private ParticipatingUser participatingUser;
    
    /**
     * 通知を既読にする
     */
    public void markAsRead() {
        this.read = true;
    }
    
    /**
     * 通知を未読にする
     */
    public void markAsUnread() {
        this.read = false;
    }
}