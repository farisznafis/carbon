import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Project } from '@/api/projects';
import { ProjectTable } from './ProjectTable';

const activeProject: Project = {
  id: 'PROJ0001',
  companyId: 'COMPANY0001',
  companyName: 'ABC株式会社',
  siteName: '東京工場',
  projectName: '太陽光発電プロジェクト',
  projectStatus: '実施中',
  methodologyVersion: '1.0',
  methodCategory: '省エネルギー',
  methodNumber: 'EN-R-002',
  methodName: '太陽光発電',
  methodMainEmission: 'CO2',
  methodCombiFeasibility: false,
};

describe('ProjectTable completion confirmation', () => {
  it('asks for confirmation before changing a project to completion', async () => {
    const user = userEvent.setup();
    const onStatusChange = vi.fn().mockResolvedValue(undefined);
    Element.prototype.hasPointerCapture = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.scrollIntoView = vi.fn();

    render(
      <ProjectTable
        projects={[activeProject]}
        onStatusChange={onStatusChange}
      />,
    );

    await user.click(screen.getByTitle('ステータスを変更'));
    await user.click(await screen.findByRole('option', { name: '終了' }));

    expect(onStatusChange).not.toHaveBeenCalled();
    expect(
      await screen.findByText('ステータスを「終了」に変更しますか？'),
    ).toBeInTheDocument();
    expect(screen.getByText('ABC株式会社')).toBeInTheDocument();
    expect(screen.getByText('東京工場')).toBeInTheDocument();
    expect(screen.getAllByText('太陽光発電プロジェクト')).toHaveLength(2);

    await user.click(
      screen.getByRole('button', { name: '終了に変更する' }),
    );

    await waitFor(() =>
      expect(onStatusChange).toHaveBeenCalledWith(activeProject, '終了'),
    );
  });

  it('prevents further changes after the project is complete', () => {
    render(
      <ProjectTable
        projects={[{ ...activeProject, projectStatus: '終了' }]}
        onStatusChange={vi.fn()}
      />,
    );

    expect(screen.getByTitle('ステータスを変更')).toBeDisabled();
    expect(screen.getByText('終了')).toBeInTheDocument();
  });
});
