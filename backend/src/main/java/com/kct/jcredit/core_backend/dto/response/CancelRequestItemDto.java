package com.kct.jcredit.core_backend.dto.response;

/**
 * 取消申請アイテムDTO
 * フロントエンドに必要な最小限の情報のみ
 */

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class CancelRequestItemDto {
    
    // 基本情報
    private Integer id;
    
    /**
     * 申請日時（表示用フォーマット）
     */
    private String requestedAt;  // "2026-03-25 09:30:00"
    
    /**
     * 取消理由
     */
    private String reason;
    
    /**
     * レコード名/ID
     */
    private String recordPk;
    
    /**
     * ステータス: pending, approved, rejected
     */
    private String status;
    
    // コンテキスト情報（一覧で便利）
    
    /**
     * 申請者名またはメール
     */
    private String requestedBy;
    
    /**
     * 拠点名
     */
    private String siteName;
    
    // 承認/却下情報（条件付き）
    
    /**
     * 承認/却下日時（approved/rejectedの場合のみ）
     */
    private String decidedAt;
    
    /**
     * 承認/却下者
     */
    private String decidedBy;
}