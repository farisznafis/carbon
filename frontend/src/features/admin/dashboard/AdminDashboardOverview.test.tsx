import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AdminDashboardOverview } from './AdminDashboardOverview';

const baseProps = {
  projects: [],
  projectsLoading: false,
  projectsError: null,
  applications: [],
  applicationTotal: 0,
  applicationsLoading: false,
  applicationsError: null,
  participatingCompanyTotal: 0,
  participatingCompaniesLoading: false,
  participatingCompaniesError: null,
  cancelRequests: [],
  cancelRequestsLoading: false,
  cancelRequestsError: null,
  batchHistory: [],
  batchHistoryLoading: false,
  notifications: [],
  notificationsLoading: false,
  notificationsError: null,
};

describe('AdminDashboardOverview', () => {
  it('keeps loaded metrics visible while an independent source is loading', () => {
    render(
      <AdminDashboardOverview
        {...baseProps}
        projects={[{
          id: 'P100',
          projectName: '太陽光プロジェクト',
          projectStatus: 'ACTIVE',
          methodologyVersion: '1.0',
          methodCategory: '省エネルギー',
          methodNumber: 'EN-S-001',
          methodName: '太陽光発電',
          methodMainEmission: '電力',
          methodCombiFeasibility: false,
        }]}
        applicationsLoading
      />,
    );

    expect(screen.getByText('登録済みプロジェクト').previousElementSibling).toHaveTextContent('1');
    expect(screen.getAllByRole('status', { name: '一覧を読み込んでいます' }).length).toBeGreaterThan(0);
    expect(screen.getByText('データ連携準備中')).toBeVisible();
    expect(screen.getByText('表示イメージ')).toBeVisible();
  });

  it('distinguishes empty action and update states from loading', () => {
    render(<AdminDashboardOverview {...baseProps} />);

    expect(screen.getByText('現在、対応が必要な項目はありません。')).toBeVisible();
    expect(screen.getByText('最近の更新はありません。')).toBeVisible();
  });
});
