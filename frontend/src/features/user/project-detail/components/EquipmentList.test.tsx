import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { SiteEquipmentHistory, SiteProjectEquipment } from '@/api/site';
import { EquipmentList } from './EquipmentList';

const { getSiteEquipmentHistory, getSiteProjectEquipmentPresignedUrl } =
  vi.hoisted(() => ({
    getSiteEquipmentHistory: vi.fn(),
    getSiteProjectEquipmentPresignedUrl: vi.fn(),
  }));

vi.mock('@/api/site', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/site')>()),
  getSiteEquipmentHistory,
  getSiteProjectEquipmentPresignedUrl,
}));

const equipment: SiteProjectEquipment = {
  equipmentId: 7,
  siteId: 'SITE-1',
  deviceNo: 'SF-TKY-001',
  equipmentOpDate: '2026-01-01',
  nominalMaxOutput: 25,
  pcsManufacturer: 'PCS Maker',
  pcsModel: 'PCS-100',
  powerCondUnitCount: 1,
  powerCondStandby: 0,
  totalInvestment: 7_800_000,
  hasBattery: false,
  hasSubsidy: false,
  files: [
    {
      id: 11,
      fileType: 'solar_contract',
      fileId: 'file-11',
      fileName: '契約確認書.pdf',
      s3Key: 'equipment/contract.pdf',
    },
  ],
};

const history: SiteEquipmentHistory = {
  id: 31,
  changeDate: '2026-08-13',
  previousEquipmentId: 6,
  monitoringStartDate: '2026-08-13',
  monitoringEndDate: null,
  isActive: true,
  registeredBy: 'operator-1',
  registeredAt: '2026-08-13T01:39:59.538Z',
  equipment: {
    ...equipment,
    files: [
      {
        id: 21,
        fileType: 'pcs_spec',
        fileId: 'file-21',
        fileName: 'PCS仕様書.pdf',
        s3Key: 'equipment/pcs.pdf',
      },
    ],
  },
};

describe('EquipmentList equipment history', () => {
  beforeEach(() => {
    getSiteEquipmentHistory.mockReset();
    getSiteProjectEquipmentPresignedUrl.mockReset();
    getSiteEquipmentHistory.mockResolvedValue([history]);
    getSiteProjectEquipmentPresignedUrl.mockResolvedValue(
      'https://storage.example.com/pcs.pdf',
    );
  });

  it('shows endpoint fields in the summary and complete details with files', async () => {
    const user = userEvent.setup();
    const anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    render(
      <EquipmentList
        siteId="SITE-1"
        equipments={[equipment]}
        onEditEquipment={vi.fn()}
      />,
    );

    expect(screen.getByText('SF-TKY-001')).toBeInTheDocument();
    expect(screen.getByText('PCSメーカー')).toBeInTheDocument();
    expect(screen.getByText('PCS Maker')).toBeInTheDocument();
    expect(screen.getByText('パワコン台数')).toBeInTheDocument();
    expect(screen.getByText('1 台')).toBeInTheDocument();
    expect(screen.getByText('総投資額')).toBeInTheDocument();
    expect(screen.getByText('7,800,000 円')).toBeInTheDocument();
    expect(screen.getByText('補助金有無')).toBeInTheDocument();
    expect(screen.queryByText('方法論番号')).not.toBeInTheDocument();
    expect(screen.queryByText('適格性')).not.toBeInTheDocument();
    expect(screen.queryByText('設備ID')).not.toBeInTheDocument();
    expect(screen.queryByText('拠点ID')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '詳細' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('基本情報')).toBeInTheDocument();
    expect(within(dialog).getByText('PCS情報')).toBeInTheDocument();
    expect(within(dialog).queryByText('設備ID')).not.toBeInTheDocument();
    expect(within(dialog).queryByText('拠点ID')).not.toBeInTheDocument();
    expect(within(dialog).getByText('契約確認書.pdf')).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: '表示' }));
    expect(getSiteProjectEquipmentPresignedUrl).toHaveBeenCalledWith(
      'equipment/contract.pdf',
    );
    expect(anchorClick).toHaveBeenCalledTimes(1);
    anchorClick.mockRestore();
  });

  it('opens history, drills into details, and reuses the cached response', async () => {
    const user = userEvent.setup();
    const anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    render(
      <EquipmentList
        siteId="SITE-1"
        equipments={[equipment]}
        onEditEquipment={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: '詳細' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: '変更履歴' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: '設備変更' }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '変更履歴' }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('設備変更履歴')).toBeInTheDocument();
    expect(getSiteEquipmentHistory).toHaveBeenCalledWith('SITE-1', 7);
    expect(within(dialog).getByText('operator-1')).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: '詳細' }));

    expect(
      within(dialog).getByText('設備変更履歴詳細'),
    ).toBeInTheDocument();
    expect(within(dialog).getByText('PCS仕様書.pdf')).toBeInTheDocument();

    await user.click(
      within(dialog).getByRole('button', {
        name: '設備変更履歴一覧へ戻る',
      }),
    );
    expect(within(dialog).getByText('設備変更履歴')).toBeInTheDocument();
    expect(within(dialog).getByText('operator-1')).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: '詳細' }));

    await user.click(within(dialog).getByRole('button', { name: '表示' }));
    expect(getSiteProjectEquipmentPresignedUrl).toHaveBeenCalledWith(
      'equipment/pcs.pdf',
    );
    expect(anchorClick).toHaveBeenCalledTimes(1);

    await user.keyboard('{Escape}');
    await user.click(screen.getByRole('button', { name: '変更履歴' }));
    expect(await screen.findByText('operator-1')).toBeInTheDocument();
    expect(getSiteEquipmentHistory).toHaveBeenCalledTimes(1);

    anchorClick.mockRestore();
  });

  it('hides history and edit actions when an equipment has no backend id', () => {
    render(
      <EquipmentList
        siteId="SITE-1"
        equipments={[{ ...equipment, equipmentId: null }]}
        onEditEquipment={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: '詳細' })).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: '変更履歴' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: '設備変更' }),
    ).not.toBeInTheDocument();
  });

  it('allows retrying a failed history request and shows the empty state', async () => {
    const user = userEvent.setup();
    getSiteEquipmentHistory
      .mockRejectedValueOnce(new Error('履歴を取得できません。'))
      .mockResolvedValueOnce([]);

    render(
      <EquipmentList
        siteId="SITE-1"
        equipments={[equipment]}
        onEditEquipment={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: '変更履歴' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      '履歴を取得できません。',
    );

    await user.click(screen.getByRole('button', { name: '再試行' }));
    expect(
      await screen.findByText('設備変更履歴はありません。'),
    ).toBeInTheDocument();
    expect(getSiteEquipmentHistory).toHaveBeenCalledTimes(2);
  });
});
