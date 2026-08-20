package com.kct.jcredit.core_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
 
/**
 * 受け付ける証憑の種類。
 *
 * 別紙10-1「会員情報」シートの「モニタリング方法」の選択肢に対応する。
 */
@Getter
@RequiredArgsConstructor
public enum EvidenceType {
 
    /** モニター表示器の撮影写真 */
    MONITOR_PHOTO("モニター表示器"),
 
    /** パワーコンディショナー表示器の撮影写真 */
    PCS_DISPLAY_PHOTO("パワーコンディショナーの表示器"),
 
    /** ICT機器による取得データ */
    ICT_DATA("ICT機器による取得データ"),
 
    /** 電力会社発行の明細書（購入電力量のお知らせ等） */
    UTILITY_STATEMENT("電力会社発行の明細書"),
 
    /** 蓄電池メーターの撮影写真 */
    BATTERY_METER_PHOTO("蓄電池メーターの撮影写真"),
 
    /** PCS実測消費電力に関する資料 */
    PCS_MEASUREMENT("PCS実測消費電力に関する資料");
 
    private final String labelJa;
}
 
