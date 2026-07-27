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
      'powerCondUnitCount',
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
