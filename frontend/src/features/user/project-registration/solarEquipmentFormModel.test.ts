import {
  getMissingEquipmentFileFieldErrors,
  getMissingEquipmentRequiredFieldErrors,
  mapEquipmentValidationFieldErrors,
  SOLAR_FORM_DEFAULT_VALUES,
} from './solarEquipmentFormModel';

describe('mapEquipmentValidationFieldErrors', () => {
  it('maps scalar and generic file errors to visible form fields', () => {
    const fieldErrors = mapEquipmentValidationFieldErrors(
      {
        equipmentOpDate: '稼働開始日を入力してください。',
        nominalMaxOutput: '公称最大出力を入力してください。',
        pcsManufacturer: 'メーカーを入力してください。',
        files: '確認資料をアップロードしてください。',
      },
      SOLAR_FORM_DEFAULT_VALUES,
    );

    expect(Object.fromEntries(
      fieldErrors.map(({ fieldName, message }) => [fieldName, message]),
    )).toEqual({
      equipmentOpDate: '稼働開始日を入力してください。',
      nominalMaxOutput: '公称最大出力を入力してください。',
      pcsManufacturer: 'メーカーを入力してください。',
      operationStartDateFile: '確認資料をアップロードしてください。',
      nominalMaxOutputFile: '確認資料をアップロードしてください。',
      pcsFile: '確認資料をアップロードしてください。',
      totalInvestmentFile: '確認資料をアップロードしてください。',
      preProjectUnitPriceFile: '確認資料をアップロードしてください。',
    });
  });

  it('includes conditional battery and subsidy file controls', () => {
    const fieldErrors = mapEquipmentValidationFieldErrors(
      {
        'equipment.files': ['空要素は許可されていません。'],
      },
      {
        ...SOLAR_FORM_DEFAULT_VALUES,
        hasBattery: 'yes',
        hasSubsidy: 'yes',
      },
    );

    expect(fieldErrors.map(({ fieldName }) => fieldName)).toEqual([
      'operationStartDateFile',
      'nominalMaxOutputFile',
      'pcsFile',
      'batteryFile',
      'totalInvestmentFile',
      'subsidyNameFile',
      'preProjectUnitPriceFile',
    ]);
  });

  it('maps tempId-prefixed backend keys to every matching form control', () => {
    const fieldErrors = mapEquipmentValidationFieldErrors(
      {
        'equipment-1785141648649::solar_contract':
          'solar_contractファイルは必須です。',
        'equipment-1785141648649::construction_invoice':
          'construction_invoiceファイルは必須です。',
        'equipment-1785141648649::electricity_bill':
          'electricity_billファイルは必須です。',
        'equipment-1785141648649::totalInvestment':
          '総投資額は0より大きい値を入力してください。',
        'equipment-1785141648649::PcsUnitCount':
          '1以上の数値を入力してください。',
        'equipment-1785141648649::equipmentOpDate': '必須項目です。',
        'equipment-1785141648649::PcsStandby': '必須項目です。',
        'equipment-1785141648649::deviceNo': '必須項目です。',
        'equipment-1785141648649::pcs_spec':
          'pcs_specファイルは必須です。',
        'equipment-1785141648649::pcsModel': '必須項目です。',
        'equipment-1785141648649::pcsManufacturer': '必須項目です。',
        'equipment-1785141648649::panel_catalog':
          'panel_catalogファイルは必須です。',
      },
      SOLAR_FORM_DEFAULT_VALUES,
    );

    expect(fieldErrors.map(({ fieldName }) => fieldName)).toEqual([
      'operationStartDateFile',
      'totalInvestmentFile',
      'preProjectUnitPriceFile',
      'totalInvestment',
      'powerCondUnitCount',
      'equipmentOpDate',
      'powerCondStandby',
      'deviceNo',
      'pcsFile',
      'pcsModel',
      'pcsManufacturer',
      'nominalMaxOutputFile',
    ]);
  });

  it('maps the Japanese operation-date validation key to equipmentOpDate', () => {
    const fieldErrors = mapEquipmentValidationFieldErrors(
      {
        'equipment-1786949086583::稼働開始日（電力受給開始日）':
          '未来の日付は入力できません。',
      },
      SOLAR_FORM_DEFAULT_VALUES,
    );

    expect(fieldErrors).toEqual([
      {
        fieldName: 'equipmentOpDate',
        message: '未来の日付は入力できません。',
      },
    ]);
  });

  it('maps a whitespace-suffixed Japanese site key to the site selector', () => {
    const fieldErrors = mapEquipmentValidationFieldErrors(
      {
        'equipment-1786956672731::拠点\r': '必須項目です。',
      },
      SOLAR_FORM_DEFAULT_VALUES,
    );

    expect(fieldErrors).toEqual([
      {
        fieldName: 'siteId',
        message: '必須項目です。',
      },
    ]);
  });

  it('maps Japanese PATCH file errors to their upload controls', () => {
    const fieldErrors = mapEquipmentValidationFieldErrors(
      {
        '1::確認資料：蓄電池仕様書・カタログ':
          '蓄電池を設置する場合は、蓄電池仕様書・カタログを添付してください。',
        '1::確認資料：工事請負契約書、請求書、領収書等':
          'construction_invoiceの添付が必要です。',
        '1::確認資料：補助金交付決定通知書':
          '補助金を受給する場合は、補助金交付決定通知書を添付してください。',
        '1::確認資料：電力会社からの請求明細書等':
          'electricity_billの添付が必要です。',
        '1::確認資料：仕様書・カタログ': 'pcs_specの添付が必要です。',
      },
      {
        ...SOLAR_FORM_DEFAULT_VALUES,
        hasBattery: 'yes',
        hasSubsidy: 'yes',
      },
    );

    expect(fieldErrors.map(({ fieldName }) => fieldName)).toEqual([
      'batteryFile',
      'totalInvestmentFile',
      'subsidyNameFile',
      'preProjectUnitPriceFile',
      'pcsFile',
    ]);
  });
});

describe('getMissingEquipmentFileFieldErrors', () => {
  it('detects an individual missing file even when the other files are selected', () => {
    const selectedFile = new File(['content'], 'document.pdf', {
      type: 'application/pdf',
    });
    const fieldErrors = getMissingEquipmentFileFieldErrors({
      ...SOLAR_FORM_DEFAULT_VALUES,
      operationStartDateFile: selectedFile,
      nominalMaxOutputFile: selectedFile,
      pcsFile: selectedFile,
      preProjectUnitPriceFile: selectedFile,
    });

    expect(fieldErrors).toEqual([
      {
        fieldName: 'totalInvestmentFile',
        message: '確認資料をアップロードしてください。',
      },
    ]);
  });

  it('requires battery and subsidy files only when those options are enabled', () => {
    const fieldErrors = getMissingEquipmentFileFieldErrors({
      ...SOLAR_FORM_DEFAULT_VALUES,
      hasBattery: 'yes',
      hasSubsidy: 'yes',
    });

    expect(fieldErrors.map(({ fieldName }) => fieldName)).toEqual([
      'operationStartDateFile',
      'nominalMaxOutputFile',
      'pcsFile',
      'batteryFile',
      'totalInvestmentFile',
      'subsidyNameFile',
      'preProjectUnitPriceFile',
    ]);
  });
});

describe('getMissingEquipmentRequiredFieldErrors', () => {
  it('returns warnings for every empty required scalar and file control', () => {
    const fieldErrors = getMissingEquipmentRequiredFieldErrors(
      SOLAR_FORM_DEFAULT_VALUES,
    );

    expect(fieldErrors.map(({ fieldName }) => fieldName)).toEqual([
      'equipmentOpDate',
      'nominalMaxOutput',
      'pcsManufacturer',
      'pcsModel',
      'deviceNo',
      'powerCondStandby',
      'hasBattery',
      'totalInvestment',
      'hasSubsidy',
      'preProjectUnitPrice',
      'operationStartDateFile',
      'nominalMaxOutputFile',
      'pcsFile',
      'totalInvestmentFile',
      'preProjectUnitPriceFile',
    ]);
  });
});
