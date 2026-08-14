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
  isJoined: true,
} as SiteProject;

describe('ProjectMembershipCard', () => {
  it('downloads the membership PDF for a joined project', async () => {
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
      screen.getByRole('button', { name: 'PDFをダウンロード' }),
    );

    expect(onDownloadPdf).toHaveBeenCalledOnce();
    expect(onDownloadPdf).toHaveBeenCalledWith(joinedProject);
  });

  it('disables download while the participation ID is unavailable', () => {
    render(
      <ProjectMembershipCard
        project={{ ...joinedProject, participationId: undefined }}
        status="joined"
        onDownloadPdf={vi.fn()}
      />,
    );

    expect(
      screen.getByRole('button', { name: 'PDFをダウンロード' }),
    ).toBeDisabled();
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
      screen.getByRole('button', { name: 'PDFをダウンロード' }),
    ).toBeDisabled();
  });
});
