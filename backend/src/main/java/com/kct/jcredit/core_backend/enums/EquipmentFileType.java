package com.kct.jcredit.core_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
 
/**
 *
 * SHOULD  : SOLAR_CONTRACT, PANEL_CATALOG, PCS_SPEC,
 *                     CONSTRUCTION_INVOICE, ELECTRICITY_BILL
 * Conditional        : BATTERY_CATALOG  (hasBattery = true)
 *                      SUBSIDY_NOTICE   (hasSubsidy = true)
 */
public enum EquipmentFileType {
 
    SOLAR_CONTRACT("solar_contract"),           // 太陽光発電電力受給契約確認書
    PANEL_CATALOG("panel_catalog"),             // 仕様書・カタログ (panel)
    PCS_SPEC("pcs_spec"),                       // 仕様書 (PCS)
    BATTERY_CATALOG("battery_catalog"),         // 仕様書・カタログ (battery) — conditional
    CONSTRUCTION_INVOICE("construction_invoice"), // 工事請負契約書・請求書等
    SUBSIDY_NOTICE("subsidy_notice"),           // 補助金交付決定通知書 — conditional
    ELECTRICITY_BILL("electricity_bill");        // 電力会社請求明細書等
 
    private final String value;
 
    EquipmentFileType(String value) {
        this.value = value;
    }
 
    // Serialisasi JSON → "solar_contract" NOT "SOLAR_CONTRACT"
    @JsonValue
    public String getValue() {
        return value;
    }
 
    // Deserialisasi dari JSON →  "solar_contract"
    @JsonCreator
    public static EquipmentFileType fromValue(String value) {
        for (EquipmentFileType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Invalid fileType: '" + value + "'. " +
                "Accepted values: solar_contract, panel_catalog, pcs_spec, " +
                "battery_catalog, construction_invoice, subsidy_notice, electricity_bill"
        );
    }
}