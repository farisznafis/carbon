dynamoDB


```mermaid
erDiagram
    solar_monitoring_table_record {
        string siteId_projectId "PK: siteId-projectId"
        string timeUpdated "SK: timeUpdated"
        number siteID "拠点ID"
        number projectID "プロジェクトID"
        number participatingUserID "参加企業ユーザID"
        string date_recorder "monitoring date 記録日"
        number ELpv "発電量 kWh"
        number ELpvr "売電量 kWh"
        string ELpv_evidence "legacy"
        string ELpvr_evidence "legacy"
        string elPvEvidenceUrl "S3 pre-signed URL"
        string elPvrEvidenceUrl "S3 pre-signed URL"
        string elpjBatterySpecificationUrl "S3 pre-signed URL"
        string elpjControlEvidenceUrl "S3 pre-signed URL"
        number ELPJ_control_value "制御機器値"
        number ELPJ_battery_value "バッテリー値"
        number ELPJ_accuracy "計測精度 %"
        string ELPJ_battery_evidence "バッテリー証拠"
        string ELPJ_control_equipment_spec_files "制御機器仕様書"
        boolean changedBattery "バッテリー変更有無"
        boolean isActive "アクティブ"
        string processedAt "処理日時"
    }
```

table: `project_document_status`


```mermaid
erDiagram
    project_document_status {
        string projectId "PK"
        boolean isRegistrationCertReceived "登録証明書受領"
        boolean isValidationReportReceived "妥当性確認報告書受領"
        boolean isVerificationReportReceived "検証報告書受領"
    }
```

table: `confirmation_items` 

```mermaid
erDiagram
    confirmation_items {
        string id "｛PK｝ UUID"
        number site_id "GSI site-index"
        number project_id "GSI project-index"
        boolean is_agree_terms
        boolean is_agree_data_use
        boolean is_agree_extra_data
        boolean is_agree_transfer
        boolean is_not_registered
        boolean is_keidanren_participation
        boolean is_emitter
        boolean is_adjusted_emissions_added
        string emitter_code
        boolean is_subject_to_periodic_report
        boolean has_energy_report
        string energy_business_code
        string factory_code
        boolean has_consent
        boolean has_checked_facility_info
        boolean has_facility_info_attached
        string created_at
        string updated_at
    }
```

---


| SQL  | カルム |   |
|-----------|--------|---------------------|
| `record_cancel_request` | `record_pk` | → `solar_monitoring_table_record.PK` |
| `record_cancel_request` | `record_sk` | → `solar_monitoring_table_record.SK` |

---

