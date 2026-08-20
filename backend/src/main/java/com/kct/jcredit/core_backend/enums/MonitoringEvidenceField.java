package com.kct.jcredit.core_backend.enums;

import java.util.List;

import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;

import java.util.Arrays;
 
import lombok.Getter;
import lombok.RequiredArgsConstructor;
 
/**
 * モニタリングデータの入力項目のうち、証憑を伴うもの。
 *
 * 出典: 方法論 EN-R-002（Ver.3.7）6～7ページ「6．モニタリング方法」1) 活動量のモニタリング
 * https://japancredit.go.jp/pdf/methodology/EN-R-002_v3.7.pdf
 */
@Getter
@RequiredArgsConstructor
public enum MonitoringEvidenceField {

    EL_PV(
            "elPv",
            "発電電力量の証憑",
            "ELPV_EVIDENCE",
            List.of(EvidenceType.MONITOR_PHOTO,
                    EvidenceType.PCS_DISPLAY_PHOTO,
                    EvidenceType.ICT_DATA),
            List.of("pdf", "csv", "png", "jpeg", "jpg")),

    EL_PVR(
            "elPvr",
            "売電電力量の証憑",
            "ELPVR_EVIDENCE",
            List.of(EvidenceType.MONITOR_PHOTO,
                    EvidenceType.PCS_DISPLAY_PHOTO,
                    EvidenceType.UTILITY_STATEMENT),
            List.of("pdf", "png", "jpeg", "jpg")),

    EL_PJ_CONTROL(
            "elPjControl",
            "PCS消費電力の証憑",
            "ELPJ_CONTROL_EVIDENCE",
            List.of(EvidenceType.MONITOR_PHOTO,
                    EvidenceType.PCS_DISPLAY_PHOTO,
                    EvidenceType.PCS_MEASUREMENT),
            List.of("pdf", "png", "jpeg", "jpg")),

    EL_PJ_BATTERY(
            "elPjBattery",
            "蓄電池充放電ロスの証憑",
            "ELPJ_BATTERY_EVIDENCE", 
            List.of(EvidenceType.BATTERY_METER_PHOTO),
            List.of("pdf", "png", "jpeg", "jpg"));

    /** APIレスポンスおよびモニタリング登録リクエストで使用する項目名 */
    private final String fieldName;

    /** 画面表示用の名称 */
    private final String labelJa;

    /** ファイルアップロード時のfileType識別子 */
    private final String fileType;

    /** 受け付ける証憑の種類 */
    private final List<EvidenceType> acceptedTypes;

    /** 許可する拡張子一覧 */
    private final List<String> allowedExtensions;

    /** fileType文字列からenumを逆引き */
    public static MonitoringEvidenceField fromFileType(String fileType) {
        return Arrays.stream(MonitoringEvidenceField.values())
                .filter(v -> v.fileType.equals(fileType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(
                        Arrays.stream(MonitoringEvidenceField.values())
                                .map(MonitoringEvidenceField::getFileType)
                                .toList(),
                        fileType));
        }
}