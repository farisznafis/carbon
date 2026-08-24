// Verifies the complete read-only review and its final confirmation actions.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { SolarEquipment } from './types';
import { ProjectRegistrationReviewStep } from './ProjectRegistrationReviewStep';

// Builds equipment with conditional battery, subsidy, local, and stored files.
function createEquipment(): SolarEquipment {
  return {
    id: 'equipment-1',
    formType: 'solar',
    solarData: {
      siteId: 'site001',
      equipmentOpDate: '2026-08-17',
      operationStartDateFile: new File(['contract'], 'contract.pdf'),
      nominalMaxOutput: '49.5',
      pcsManufacturer: 'Omron',
      pcsModel: 'KP49R',
      powerCondUnitCount: '1',
      deviceNo: 'DEVICE-001',
      powerCondStandby: '0.05',
      hasBattery: 'yes',
      batteryMaker: 'Tesla',
      batteryModel: 'Powerwall',
      batteryCapacity: '13.5',
      totalInvestment: '7800000',
      hasSubsidy: 'yes',
      subsidyName: '導入補助金',
      subsidyAmount: '1500000',
      preProjectUnitPrice: '26.5',
    },
    files: [
      {
        fileType: 'pcs_spec',
        fileId: 'pcs-file-id',
        fileName: 'stored-pcs.pdf',
        s3Key: 'tmp/stored-pcs.pdf',
      },
    ],
  };
}

describe('ProjectRegistrationReviewStep', () => {
  it('shows all confirmation sections and conditional values', () => {
    render(
      <ProjectRegistrationReviewStep
        projectName="関東太陽光発電プロジェクト"
        siteName="東京工場"
        equipments={[createEquipment()]}
        methodologyNumber="003"
        methodologyVersion="Ver.2.0"
        agreementValues={{
          useRegisteredInfo: true,
          provideAdditionalInfo: true,
          transferEnvironmentalValue: true,
          notRegisteredElsewhere: true,
          keidanrenParticipation: 'participating',
          specifiedEmitterStatus: 'isSpecifiedEmitter',
          specifiedEmitterCode: '123456789',
          emissionAdditionAgreement: true,
          energyRationalizationCompliance: 'applicable',
          energyLawType: 'type1',
          energyLawBusinessNumber: '1234567',
          energyLawFactoryNumber: '7654321',
        }}
        contractValues={{
          businessName: 'ABC株式会社',
          representativeName: '山田太郎',
          representativePosition: '代表取締役',
          contractAgreed: true,
        }}
        finalTermsValues={{ finalTermsAgreed: true }}
        isSubmitting={false}
        onBack={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { name: '申請内容の確認' })).toBeVisible();
    expect(screen.getByText('関東太陽光発電プロジェクト')).toBeVisible();
    expect(screen.getByText('東京工場')).toBeVisible();
    expect(screen.getByText('003 / Ver.2.0')).toBeVisible();
    expect(screen.getByText('contract.pdf')).toBeVisible();
    expect(screen.getByText('stored-pcs.pdf')).toBeVisible();
    expect(screen.getByText('Tesla')).toBeVisible();
    expect(screen.getByText('導入補助金')).toBeVisible();
    expect(screen.getByText('123456789')).toBeVisible();
    expect(screen.getByText('ABC株式会社')).toBeVisible();
    expect(screen.getByText('入会規約')).toBeVisible();
  });

  it('runs back and submit actions and prevents actions while submitting', async () => {
    const user = userEvent.setup();
    const onBack = vi.fn();
    const onSubmit = vi.fn();
    const { rerender } = render(
      <ProjectRegistrationReviewStep
        projectName="Project"
        siteName="Site"
        equipments={[createEquipment()]}
        methodologyNumber="003"
        methodologyVersion="Ver.2.0"
        agreementValues={{}}
        contractValues={{}}
        finalTermsValues={{}}
        isSubmitting={false}
        onBack={onBack}
        onSubmit={onSubmit}
      />,
    );

    await user.click(screen.getByRole('button', { name: '戻る' }));
    await user.click(screen.getByRole('button', { name: '申請する' }));
    expect(onBack).toHaveBeenCalledOnce();
    expect(onSubmit).toHaveBeenCalledOnce();

    rerender(
      <ProjectRegistrationReviewStep
        projectName="Project"
        siteName="Site"
        equipments={[createEquipment()]}
        methodologyNumber="003"
        methodologyVersion="Ver.2.0"
        agreementValues={{}}
        contractValues={{}}
        finalTermsValues={{}}
        isSubmitting
        onBack={onBack}
        onSubmit={onSubmit}
      />,
    );

    expect(screen.getByRole('button', { name: '戻る' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '送信中...' })).toBeDisabled();
  });
});
