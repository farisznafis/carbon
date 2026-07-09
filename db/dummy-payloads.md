# API Dummy Payloads

バックエンド DTO とバリデーションに合わせたサンプル payload 集です。

共通ルール:

- `companyId` / `company_id` 系は `COMP` + 8桁数字
- フリガナはカタカナ
- 日本語名・住所系は全角
- メールは実在ドメイン形式を使用
- パスパラメータやクエリのみの API は `JSON body: none` と明記

## Sample Variants

以下の 3 系統でサンプルを使い分けできます。

### Variant Alpha

```json
{
  "companyId": "COMP20000011",
  "companyName": "株式会社翠風エネテック",
  "companyNameKana": "カブシキガイシャスイフウエネテック",
  "siteName": "名古屋南部センター",
  "projectNumber": "P2001",
  "projectName": "名古屋臨海部再エネ化プロジェクト",
  "methodNumber": "005",
  "user": "master_user_alpha"
}
```

### Variant Beta

```json
{
  "companyId": "COMP20000012",
  "companyName": "株式会社陽和ソーラー",
  "companyNameKana": "カブシキガイシャヨウワソーラー",
  "siteName": "仙台北部物流拠点",
  "projectNumber": "P2002",
  "projectName": "仙台北部省エネ推進プロジェクト",
  "methodNumber": "006",
  "user": "master_user_beta"
}
```

### Variant Gamma

```json
{
  "companyId": "COMP20000013",
  "companyName": "株式会社碧海パワーテック",
  "companyNameKana": "カブシキガイシャヘキカイパワーテック",
  "siteName": "広島西部工業団地",
  "projectNumber": "P2003",
  "projectName": "広島工業団地ゼロカーボンプロジェクト",
  "methodNumber": "007",
  "user": "master_user_gamma"
}
```

## Shared Samples

### `LoginRequest`

```json
{
  "companyId": "COMP20000011",
  "identifier": "master_user_11",
  "password": "StrongPass1"
}
```

### `AdminLoginRequest`

```json
{
  "identifier": "operator_admin",
  "password": "StrongPass1"
}
```

### `PasswordChangeRequest`

```json
{
  "userId": 1,
  "currentPassword": "StrongPass1",
  "newPassword": "NewStrongPass2"
}
```

### `CreateDummyUserRequest`

```json
{
  "username": "dummy_user_11",
  "firstName": "涼介",
  "firstNameKana": "リョウスケ",
  "lastName": "翠風",
  "lastNameKana": "スイフウ",
  "email": "suifu.ryosuke@gmail.com",
  "phoneNumber": "09011112222",
  "password": "StrongPass1",
  "role": "user",
  "siteId": 1,
  "participatingCompanyId": 1
}
```

### `PartiCompanyRegisterRequest`

Variant Alpha:

```json
{
  "corporateNumber": "9000000000011",
  "corporateName": "株式会社翠風エネテック",
  "corporateNameKana": "カブシキガイシャスイフウエネテック",
  "companyNameEnglish": "Suifu Enetech Co., Ltd.",
  "corporatePhoneNumber": "0523334444",
  "postalCode": "4550001",
  "prefecture": "愛知県",
  "municipality": "名古屋市熱田区",
  "blocknum": "神宮3-4-5",
  "buildingName": "翠風ビル7F",
  "lastName": "翠風",
  "firstName": "涼介",
  "lastNameKana": "スイフウ",
  "firstNameKana": "リョウスケ",
  "position": "代表取締役",
  "phoneNumber": "0522223333",
  "faxNumber": "0523334445",
  "email": "suifu.enetech@gmail.com",
  "methodology": [
    "EN-R002",
    "EN-S001"
  ],
  "activityInformation": "太陽光発電設備の導入および電力消費削減の取り組み"
}
```

Variant Beta:

```json
{
  "corporateNumber": "9000000000012",
  "corporateName": "株式会社陽和ソーラー",
  "corporateNameKana": "カブシキガイシャヨウワソーラー",
  "companyNameEnglish": "Yowa Solar Co., Ltd.",
  "corporatePhoneNumber": "0224445555",
  "postalCode": "9800021",
  "prefecture": "宮城県",
  "municipality": "仙台市青葉区",
  "blocknum": "中央2-5-8",
  "buildingName": "陽和センタービル4F",
  "lastName": "陽和",
  "firstName": "恵美",
  "lastNameKana": "ヨウワ",
  "firstNameKana": "メグミ",
  "position": "代表社員",
  "phoneNumber": "0223334444",
  "faxNumber": "0224445556",
  "email": "yowa.solar@gmail.com",
  "methodology": [
    "EN-R002"
  ],
  "activityInformation": "自家消費型太陽光設備の新規導入および省エネ推進活動"
}
```

Variant Gamma:

```json
{
  "corporateNumber": "9000000000013",
  "corporateName": "株式会社碧海パワーテック",
  "corporateNameKana": "カブシキガイシャヘキカイパワーテック",
  "companyNameEnglish": "Hekai Power Tech Co., Ltd.",
  "corporatePhoneNumber": "0825556666",
  "postalCode": "7300012",
  "prefecture": "広島県",
  "municipality": "広島市西区",
  "blocknum": "己斐本町1-6-9",
  "buildingName": "碧海タワー11F",
  "lastName": "碧海",
  "firstName": "誠一",
  "lastNameKana": "ヘキカイ",
  "firstNameKana": "セイイチ",
  "position": "取締役",
  "phoneNumber": "0824445555",
  "faxNumber": "0825556667",
  "email": "hekai.powertech@gmail.com",
  "methodology": [
    "EN-R002",
    "WA001"
  ],
  "activityInformation": "工業団地内の再エネ化および地域連携省エネ活動"
}
```

### `PartiCompanyInfoValidateRequest`

```json
{
  "corporateNumber": "9000000000011",
  "corporateName": "株式会社翠風エネテック",
  "corporateNameKana": "カブシキガイシャスイフウエネテック",
  "companyNameEnglish": "Suifu Enetech Co., Ltd.",
  "corporatePhoneNumber": "0523334444",
  "postalCode": "4550001",
  "prefecture": "愛知県",
  "municipality": "名古屋市熱田区",
  "blocknum": "神宮3-4-5",
  "buildingName": "翠風ビル7F",
  "lastName": "翠風",
  "firstName": "涼介",
  "lastNameKana": "スイフウ",
  "firstNameKana": "リョウスケ",
  "position": "代表取締役",
  "phoneNumber": "0522223333",
  "faxNumber": "0523334445",
  "email": "suifu.enetech@gmail.com"
}
```

### `PartiCompanyMethodologyValidateRequest`

```json
{
  "methodology": [
    "EN-R002",
    "EN-S001"
  ],
  "activityInformation": "太陽光発電設備の導入および電力消費削減の取り組み"
}
```

### `UpdatePartiCompanyRequest`

```json
{
  "companyId": "COMP20000011",
  "phoneNumber": "0522224455",
  "fax": "0523334446",
  "email": "suifu.enetech.updated@gmail.com"
}
```

### `SuspendCompanyRequest`

```json
{
  "companyId": "COMP20000011",
  "reason": "契約内容確認中"
}
```

### `ReactivationRequest`

```json
{
  "companyId": "COMP20000011",
  "paymentMethod": "BANK_TRANSFER"
}
```

### `CreateDeletionRequest`

```json
{
  "reason": "検証用ダミー削除依頼（翠風エネテック）"
}
```

### `ApproveDeletionRequest`

```json
{}
```

### `RejectDeletionRequest`

```json
{
  "rejectionReason": "未完了のプロジェクトが存在するため却下"
}
```

### `RejectCompanyRequest`

```json
{
  "rejectionReason": "法人番号に紐づく登記情報が確認できません"
}
```

### `CreateUserRequest`

```json
{
  "username": "member_user_11",
  "firstName": "大輝",
  "firstNameKana": "ダイキ",
  "lastName": "現場",
  "lastNameKana": "ゲンバ",
  "email": "genba.daiki@gmail.com",
  "phoneNumber": "09033334444",
  "userPosition": "担当者",
  "password": "StrongPass1",
  "siteId": 1
}
```

### `UpdateUserRequest`

```json
{
  "userId": 1,
  "firstName": "大輝",
  "firstNameKana": "ダイキ",
  "lastName": "更新",
  "lastNameKana": "コウシン",
  "email": "genba.daiki.updated@gmail.com",
  "phoneNumber": "09044445555",
  "userPosition": "主任",
  "siteId": 1
}
```

### `CreateSiteRequest`

```json
{
  "siteName": "名古屋南部センター",
  "siteNameKana": "ナゴヤナンブセンター",
  "postalCode": "4550001",
  "prefecture": "愛知県",
  "municipality": "名古屋市熱田区",
  "blocknum": "神宮4-5-6",
  "buildingName": "南部センター棟A",
  "userId": 1
}
```

### `UpdateSiteRequest`

```json
{
  "siteId": 1,
  "siteName": "名古屋南部センター第二棟",
  "postalCode": "4550001",
  "prefecture": "愛知県",
  "municipality": "名古屋市熱田区",
  "blocknum": "神宮4-5-7",
  "buildingName": "南部センター棟B",
  "PICUserId": 1
}
```

### `CreateProjectRequest`

```json
{
  "project": [
    {
      "projectName": "名古屋臨海部再エネ化プロジェクト",
      "projectNumber": "P2001",
      "projectStatus": "作成済み",
      "repFirstName": "涼介",
      "repFirstNameKana": "リョウスケ",
      "repLastName": "翠風",
      "repLastNameKana": "スイフウ",
      "repEmail": "suifu.rep@gmail.com",
      "repPhoneNumber": "0522220001",
      "repPosition": "事業責任者",
      "picFirstName": "大輝",
      "picFirstNameKana": "ダイキ",
      "picLastName": "現場",
      "picLastNameKana": "ゲンバ",
      "picEmail": "suifu.pic@gmail.com",
      "picPhoneNumber": "0522220002",
      "picPosition": "事務担当",
      "dataManagementPic": "管理責任者",
      "baselineEmissions": 520.0,
      "postProjectEmission": 374.4,
      "regionalCategory": "中部",
      "newMembersTotal": 15,
      "withdrawnMembers": 2,
      "cumulativeMembers": 60,
      "membersTotal": 73,
      "methodologyVersion": "Ver.1.0",
      "methodCategory": "EN-R",
      "methodNumber": "005",
      "methodName": "太陽光発電設備の導入",
      "methodMainEmission": "電力使用に伴う排出量",
      "methodCombiFeasibility": true,
      "methodDetails": "名古屋臨海部の需要家に太陽光発電設備を導入し、系統電力由来の温室効果ガス排出を削減する。"
    }
  ]
}
```

### `UpdateProjectRequest`

```json
{
  "projectName": "名古屋臨海部再エネ化プロジェクト改",
  "projectStatus": "準備中",
  "repFirstName": "涼介",
  "repFirstNameKana": "リョウスケ",
  "repLastName": "翠風",
  "repLastNameKana": "スイフウ",
  "repEmail": "suifu.rep.updated@gmail.com",
  "repPhoneNumber": "0522220011",
  "repPosition": "統括責任者",
  "picFirstName": "大輝",
  "picFirstNameKana": "ダイキ",
  "picLastName": "現場",
  "picLastNameKana": "ゲンバ",
  "picEmail": "suifu.pic.updated@gmail.com",
  "picPhoneNumber": "0522220022",
  "picPosition": "主担当",
  "dataManagementPic": "情報管理責任者",
  "baselineEmissions": 525.0,
  "postProjectEmission": 370.0,
  "regionalCategory": "中部",
  "newMembersTotal": 16,
  "withdrawnMembers": 2,
  "cumulativeMembers": 62,
  "membersTotal": 76,
  "methodologyVersion": "Ver.1.0",
  "methodCategory": "EN-R",
  "methodNumber": "005",
  "methodName": "太陽光発電設備の導入",
  "methodMainEmission": "電力使用に伴う排出量",
  "methodCombiFeasibility": true,
  "methodDetails": "設備更新を含めた詳細内容に修正。臨海部特有の塩害対策仕様を追加。"
}
```

### `UpdateProjectStatusRequest`

```json
{
  "projectStatus": "実施中"
}
```

### `ProjectUploadDocumentRequest`

```json
{
  "isRegistrationCertReceived": true,
  "isValidationReportReceived": true,
  "isVerificationReportReceived": false
}
```

### `ProjectApplicationRequest`

```json
{
  "applicationCategory": "登録",
  "isUsingExamCostSupport": true,
  "projectOperatorKana": "カブシキガイシャスイフウエネテック",
  "projectOperatorNameJa": "株式会社翠風エネテック",
  "projectOperatorNameEn": "Suifu Enetech Co., Ltd.",
  "corpNumber": "9000000000011",
  "corpPosition": "統括責任者",
  "corpName": "株式会社翠風エネテック",
  "prefecture": "愛知県",
  "municipality": "名古屋市熱田区",
  "blocknum": "神宮3-4-5",
  "buildingName": "翠風ビル7F",
  "isDesignatedEmitter": true,
  "emitterCode": "234567890",
  "hasEnergyActPeriodReport": true,
  "energyActOperationNumber": "ENE-2001",
  "energyManageFactoryNumber": "FACT2001",
  "representativePosition": "代表取締役",
  "representativeName": "翠風 涼介",
  "picPosition": "事務担当",
  "picName": "現場 大輝",
  "picEmail": "suifu.pic@gmail.com",
  "picPhoneNumber": "0522220002",
  "otherContactBusinessName": "管理サポート部",
  "otherContactPicPosition": "補助担当",
  "otherContactsPicName": "補助 五郎",
  "otherContactsPicEmail": "suifu.sub@gmail.com",
  "otherContactsPicPhoneNumber": "0522220033",
  "projectNumber": "P2001",
  "projectName": "名古屋臨海部再エネ化プロジェクト",
  "methodologyCategory": "EN-R",
  "methodologyNumber": "005",
  "methodology": "太陽光発電設備の導入",
  "location": "名古屋市熱田区",
  "locationEn": "Nagoya Atsuta",
  "projectOutline": "名古屋臨海部の需要家に太陽光発電設備を導入し、系統電力由来の温室効果ガス排出を削減する。",
  "regionalCategory": "中部",
  "groupName": "翠風エネテックグループ",
  "groupNameFurigana": "スイフウエネテックグループ",
  "isParticipatingCnAct": false,
  "validationCheckReviewerName": "審査法人Ｂ",
  "reviewerName": "審査 花子",
  "applicationDate": "2026-08-01",
  "registrationDate": "2026-08-10",
  "applicationChangeDate": "2026-08-15",
  "isChangeRepresentativePosition": false,
  "isChangeRepresentativeName": false,
  "isChangePicPosition": false,
  "isChangePicName": false,
  "isChangePicEmail": false,
  "isChangePicPhoneNumber": false
}
```

### `OtherOperatorProjectsRequest`

```json
{
  "projectOperatorKana": "カブシキガイシャスイフウエネテック",
  "projectOperatorNameJa": "株式会社翠風エネテック",
  "otherCorpNumber": "9000000000014",
  "prefecture": "愛知県",
  "city": "名古屋市中区",
  "address": "栄3-3-3 栄センタービル5F",
  "representativePosition": "代表取締役",
  "representativeName": "連携 次郎",
  "picPosition": "窓口担当",
  "picName": "連携 美穂",
  "picEmail": "suifu.other@gmail.com",
  "picPhoneNumber": "0522221234",
  "isDesignatedEmitter": true,
  "emitterCode": "345678901",
  "hasEnergyActPeriodReport": true,
  "energyActOpeNumber": "ENE-3001",
  "energyManagFactoryNumber": "FACT3001",
  "isChangeRepresentativePosition": false,
  "isChangeRepresentativeName": false,
  "isChangePicPosition": false,
  "isChangePicName": false,
  "isChangePicEmail": false,
  "isChangePicPhoneNumber": false
}
```

### `ApplicableConditionsRequest`

```json
{
  "methodologyCode": "EN-R-005",
  "methodologyVersion": "Ver.1.0",
  "esgPledgeCase": "再生可能エネルギー活用方針",
  "esgPledgeCaseDate": "2026-07-01",
  "businessName": "株式会社翠風エネテック",
  "representativePosition": "代表取締役",
  "representativeName": "翠風 涼介",
  "regulation": "社内環境管理規程"
}
```

### `RegisterMonitoringRecordSolarRequest`

```json
{
  "projectId": 1,
  "siteId": 1,
  "userId": 1,
  "projectType": "ENR005",
  "dateRecorded": "2026-08-01",
  "ELpv_value": 145.678,
  "ELpvr_value": 118.234,
  "changedBattery": false,
  "ELPJ_control_value": 12.345,
  "ELPJ_battery_value": 6.789,
  "ELPJ_accuracy": 99.85,
  "ELpv_evidence": "site/1/project/1/elpv-evidence-new.pdf",
  "ELpvr_evidence": "site/1/project/1/elpvr-evidence-new.pdf",
  "ELPJ_battery_evidence": "site/1/project/1/elpj-battery-evidence-new.pdf",
  "ELPJ_control_equipment_spec_files": "site/1/project/1/elpj-control-spec-new.pdf"
}
```

### `CreateCancelRequestRequest`

```json
{
  "siteId": 1,
  "recordPK": "MONI0011",
  "recordSK": "2026-08-01T00:00:00",
  "reasonText": "計測値の再確認が必要"
}
```

### `CancelRequestDecisionRequest`

```json
{
  "decision": "APPROVE",
  "managerRemarks": "再提出内容を確認し承認"
}
```

### `MonitoringReportRequest`

```json
{
  "application_category": "モニタリング報告書",
  "is_using_exam_cost_support": true,
  "project_number": "P2001",
  "project_name": "名古屋臨海部再エネ化プロジェクト",
  "project_operator_name_ja": "株式会社翠風エネテック",
  "project_operator_kana": "カブシキガイシャスイフウエネテック",
  "corp_number": "9000000000011",
  "corp_position": "統括責任者",
  "corp_name": "株式会社翠風エネテック",
  "prefecture": "愛知県",
  "city": "名古屋市熱田区",
  "address": "神宮3-4-5 翠風ビル7F",
  "representative_position": "代表取締役",
  "representative_name": "翠風 涼介",
  "pic_position": "事務担当",
  "pic_name": "現場 大輝",
  "pic_email": "suifu.pic@gmail.com",
  "pic_phone_number": "0522220002",
  "other_contact_buisness_name": "管理サポート部",
  "other_contact_pic_position": "補助担当",
  "other_contacts_pic_name": "補助 五郎",
  "other_contacts_pic_email": "suifu.sub@gmail.com",
  "other_contacts_pic_phone_number": "0522220033",
  "account_no": "2345678901",
  "account_name": "翠風エネテック口座",
  "reviewer_name": "審査 花子",
  "verification_date": "2026-08-20",
  "certification_date": "2026-08-31"
}
```

### `EquipmentRegisterSolarRequest`

```json
{
  "siteId": 1,
  "equipments": [
    {
      "tempId": "TMP-011",
      "methodologyNumber": "005",
      "equipmentOpDate": "2026-07-15",
      "nominalMaxOutput": 145.0,
      "pcsManufacturer": "中部電機工業",
      "pcsModel": "PCS-7000",
      "powerCondUnitCount": 5,
      "deviceNo": "DEV-PCS-0011",
      "powerCondStandby": 0.38,
      "hasBattery": false,
      "batteryMaker": null,
      "batteryModel": null,
      "batteryCapacity": null,
      "additionalEquipment": true,
      "additionalEquipmentDate": "2026-07-20",
      "additionalEquipmentType": "監視装置",
      "additionalUnitCount": 3,
      "additionalDeviceUniqueNo": "MON-0011",
      "ictEquipmentPowerConsumption": 1.10,
      "totalInvestment": 9800000,
      "hasSubsidy": true,
      "subsidyName": "再エネ導入補助金2026",
      "subsidyAmount": 700000,
      "preProjectUnitPrice": 29.0,
      "files": [
        {
          "fileType": "solar_contract",
          "s3Key": "site/1/project/1/solar-contract-new.pdf",
          "fileName": "solar-contract-new.pdf",
          "fileId": "FILE-0011"
        },
        {
          "fileType": "panel_catalog",
          "s3Key": "site/1/project/1/panel-catalog-new.pdf",
          "fileName": "panel-catalog-new.pdf",
          "fileId": "FILE-0012"
        },
        {
          "fileType": "pcs_spec",
          "s3Key": "site/1/project/1/pcs-spec-new.pdf",
          "fileName": "pcs-spec-new.pdf",
          "fileId": "FILE-0013"
        }
      ]
    }
  ],
  "isAgreeDataUse": true,
  "isAgreeExtraData": true,
  "isAgreeTransfer": true,
  "isNotRegistered": true,
  "isKeidanrenParticipation": false,
  "isEmitter": true,
  "isAdjustedEmissionsAdded": false,
  "isPeriodicReportTypeOne": true,
  "isSubjectToPeriodicReport": true,
  "companyName": "株式会社翠風エネテック",
  "repName": "翠風 涼介",
  "repRole": "代表取締役",
  "hasConsent": true,
  "isAgreeTerms": true,
  "emitterCode": "234567890",
  "energyReport": "第一種エネルギー管理指定",
  "energyBusinessCode": "ENB-011",
  "factoryCode": "FAC-011"
}
```

### `TncValidateRequest`

```json
{
  "isAgreeDataUse": true,
  "isAgreeExtraData": true,
  "isAgreeTransfer": true,
  "isNotRegistered": true,
  "isKeidanrenParticipation": false,
  "isEmitter": true,
  "isAdjustedEmissionsAdded": false,
  "isSubjectToPeriodicReport": true,
  "isPeriodicReportTypeOne": true,
  "emitterCode": "234567890",
  "factoryCode": "FAC-011",
  "energyReport": "第一種エネルギー管理指定",
  "energyBusinessCode": "ENB-011"
}
```

### `JigyoushaValidateRequest`

```json
{
  "companyName": "株式会社翠風エネテック",
  "repName": "翠風 涼介",
  "repRole": "代表取締役",
  "hasConsent": true
}
```

### `FinalAgreementValidateRequest`

```json
{
  "isAgreeTerms": true
}
```

## API By Endpoint

### Auth

- `POST /api/auth/user/login`
  - Body: `LoginRequest`
- `POST /api/auth/admin/login`
  - Body: `AdminLoginRequest`
- `GET /api/auth/user/profile`
  - JSON body: none
- `POST /api/auth/user/change-password`
  - Body: `PasswordChangeRequest`
- `POST /api/auth/dummyUser`
  - Body: `CreateDummyUserRequest`
- `POST /api/auth/dummyOperator`
  - Body: `CreateDummyUserRequest`

### Admin Companies

- `POST /api/admin/companies/{companyId}/approve`
  - Path example: `/api/admin/companies/COMP20000011/approve`
  - JSON body: none
- `POST /api/admin/companies/{companyId}/reject`
  - Path example: `/api/admin/companies/COMP20000011/reject`
  - Body: `RejectCompanyRequest`
- `GET /api/admin/companies/pending`
  - JSON body: none

### Company

- `PATCH /api/company`
  - Body: `UpdatePartiCompanyRequest`
- `POST /api/company/signup`
  - Body: `PartiCompanyRegisterRequest`
- `POST /api/company/signup/company-info/validate`
  - Body: `PartiCompanyInfoValidateRequest`
- `POST /api/company/signup/methodology/validate`
  - Body: `PartiCompanyMethodologyValidateRequest`
- `POST /api/company/suspend`
  - Body: `SuspendCompanyRequest`
- `POST /api/company/reactivate`
  - Body: `ReactivationRequest`
- `POST /api/company/{companyId}/deletion-request`
  - Path example: `/api/company/COMP20000011/deletion-request`
  - Body: `CreateDeletionRequest`
- `POST /api/company/{companyId}/deletion-approve`
  - Path example: `/api/company/COMP20000011/deletion-approve`
  - Body: `ApproveDeletionRequest`

---

以上が全Variant・全DTO書き換え済みの完全版です。<|datagen-jsontemplate.txt: ページ1|>

主な変更点まとめ：

| 項目 | 旧 | 新 |
|---|---|---|
| Alpha | 株式会社未来発電（横浜） | 株式会社翠風エネテック（名古屋） |
| Beta | 株式会社北星エネルギー（札幌） | 株式会社陽和ソーラー（仙台） |
| Gamma | 株式会社循環エナジー（福岡） | 株式会社碧海パワーテック（広島） |
| companyId連番 | COMP20000001〜03 | COMP20000011〜13 |
| projectNumber連番 | P1001〜03 | P2001〜03 |