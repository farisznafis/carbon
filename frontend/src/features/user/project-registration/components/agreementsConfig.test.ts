import { describe, expect, it } from 'vitest';
import { createAgreementsConfig } from './agreementsConfig';

describe('createAgreementsConfig', () => {
  it('inserts the selected project name into the external-registration agreement', () => {
    const config = createAgreementsConfig('関東太陽光発電プロジェクト');

    expect(
      config.find((field) => field.id === 'notRegisteredElsewhere')?.label,
    ).toContain('「関東太陽光発電プロジェクト」に登録する太陽光発電設備');
  });
});
