import {
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
});
