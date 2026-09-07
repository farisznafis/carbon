import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { SiteProject } from '@/api/site';
import { ProjectMembershipCard } from './ProjectMembershipCard';

const joinedProject = {
  id: 'PROJ001',
  participationId: 42,
  projectName: '太陽光発電プロジェクト',
  projectNumber: 'P001',
  projectStatus: '実施中',
  isCompanyJoined: true,
  hasSiteRelation: true,
  regionalCategory: '関東',
  methodNumber: '003',
  methodologyVersion: 'Ver.2.0',
  methodName: '自家消費型太陽光発電設備の導入',
  methodMainEmission: '事業所における購入電力使用に伴う排出量',
} as SiteProject;

describe('ProjectMembershipCard', () => {
  it('shows the regional category and combined methodology reference', () => {
    render(
      <ProjectMembershipCard project={joinedProject} status="joined" />,
    );

    expect(screen.getByText('関東')).toBeInTheDocument();
    expect(screen.getByText('方法論番号・バージョン')).toBeInTheDocument();
    expect(screen.getByText('003 / Ver.2.0')).toBeInTheDocument();
  });

  it.each([
    ['003', '', '003'],
    ['', 'Ver.2.0', 'Ver.2.0'],
    ['', '', '-'],
  ])(
    'formats methodology number %s and version %s as %s',
    (methodNumber, methodologyVersion, expected) => {
      render(
        <ProjectMembershipCard
          project={{
            ...joinedProject,
            regionalCategory: undefined,
            methodNumber,
            methodologyVersion,
          }}
          status="joined"
        />,
      );

      expect(screen.queryByText('関東')).not.toBeInTheDocument();
      expect(screen.getByText(expected)).toBeInTheDocument();
    },
  );

  it('opens the membership PDF for a joined project', async () => {
    const user = userEvent.setup();
    const onDownloadPdf = vi.fn();

    render(
      <ProjectMembershipCard
        project={joinedProject}
        status="joined"
        onDownloadPdf={onDownloadPdf}
      />,
    );

    await user.click(
      screen.getByRole('button', { name: 'PDFを表示' }),
    );

    expect(onDownloadPdf).toHaveBeenCalledOnce();
    expect(onDownloadPdf).toHaveBeenCalledWith(joinedProject);
  });

  it('hides the PDF action when no download handler is provided', () => {
    render(
      <ProjectMembershipCard project={joinedProject} status="joined" />,
    );

    expect(
      screen.queryByRole('button', { name: 'PDFを表示' }),
    ).not.toBeInTheDocument();
  });

  it('disables download while the PDF request is running', () => {
    render(
      <ProjectMembershipCard
        project={joinedProject}
        status="joined"
        onDownloadPdf={vi.fn()}
        isDownloadingPdf
      />,
    );

    expect(
      screen.getByRole('button', { name: 'PDFを表示' }),
    ).toBeDisabled();
  });
});
