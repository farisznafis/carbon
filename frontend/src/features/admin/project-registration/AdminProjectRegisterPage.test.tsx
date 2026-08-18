import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const adminProjectApiMocks = vi.hoisted(() => ({
  createAdminProject: vi.fn(),
  createAdminProjectPlanApplication: vi.fn(),
  getProjectPlanAutofillByProject: vi.fn(),
  getProjectPlanAutofillDefaults: vi.fn(),
  getProjectPlanForm: vi.fn(),
  getProjectPlanProgress: vi.fn(),
  saveProjectPlanForm: vi.fn(),
  updateAdminProject: vi.fn(),
}));

vi.mock('@/api/admin/projects', () => adminProjectApiMocks);

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'admin-1' } }),
}));

vi.mock('@/hooks/use-toast', () => ({
  toast: vi.fn(),
}));

vi.mock('./forms/ProjectMultiPageForm', () => ({
  MultiStepForm: ({ step }: { step: number }) => (
    <div data-testid="project-form-step">step-{step}</div>
  ),
}));

vi.mock('./forms/TemporaryAdminProjectAutofillButton', () => ({
  TemporaryAdminProjectAutofillButton: () => null,
  TemporaryAdminProjectApplicationAutofillButton: () => null,
}));

import { AdminProjectRegister } from './AdminProjectRegisterPage';

describe('AdminProjectRegister', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminProjectApiMocks.createAdminProject.mockResolvedValue([
      { id: 'project-1' },
    ]);
    adminProjectApiMocks.getProjectPlanAutofillDefaults.mockResolvedValue({
      data: {},
    });
    adminProjectApiMocks.getProjectPlanAutofillByProject.mockResolvedValue({
      data: {},
    });
    adminProjectApiMocks.getProjectPlanForm.mockResolvedValue({
      data: {},
      readOnlyFields: [],
    });
    adminProjectApiMocks.getProjectPlanProgress.mockResolvedValue({
      totalForms: 3,
      completedForms: 0,
      nextIncompleteSlug: 'application',
      forms: [],
    });
    adminProjectApiMocks.saveProjectPlanForm.mockResolvedValue({
      status: 'COMPLETE',
    });
  });

  it('creates and validates the project before advancing from the first step', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    const nextButton = screen.getByRole('button', { name: '次へ' });
    expect(nextButton).toBeEnabled();

    await user.click(nextButton);

    await waitFor(() => {
      expect(adminProjectApiMocks.createAdminProject).toHaveBeenCalledTimes(1);
    });
    expect(
      adminProjectApiMocks.getProjectPlanAutofillByProject,
    ).toHaveBeenCalledWith('project-1');
    expect(
      adminProjectApiMocks.getProjectPlanAutofillDefaults,
    ).toHaveBeenCalledTimes(1);
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-2',
    );
  });

  it('stages each plan page before advancing to the next one', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    // Step 1 -> 2 saves the project record, not a plan page.
    await user.click(screen.getByRole('button', { name: '次へ' }));
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-2',
    );
    expect(adminProjectApiMocks.saveProjectPlanForm).not.toHaveBeenCalled();

    // Step 2 -> 3 stages the application page.
    await user.click(screen.getByRole('button', { name: '次へ' }));
    await waitFor(() => {
      expect(adminProjectApiMocks.saveProjectPlanForm).toHaveBeenCalledWith(
        'project-1',
        'application',
        expect.any(Object),
      );
    });
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-3',
    );

    // Step 3 -> 4 stages the other-operator page.
    await user.click(screen.getByRole('button', { name: '次へ' }));
    await waitFor(() => {
      expect(adminProjectApiMocks.saveProjectPlanForm).toHaveBeenCalledWith(
        'project-1',
        'other-operator-projects',
        expect.any(Object),
      );
    });
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-4',
    );
  });

  it('does not advance past a page the server rejected', async () => {
    const user = userEvent.setup();
    adminProjectApiMocks.saveProjectPlanForm.mockRejectedValue(
      new Error('入力内容に誤りがあります。'),
    );

    render(
      <MemoryRouter>
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: '次へ' }));
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-2',
    );

    await user.click(screen.getByRole('button', { name: '次へ' }));
    await waitFor(() => {
      expect(adminProjectApiMocks.saveProjectPlanForm).toHaveBeenCalled();
    });

    // The page was not stored, so the operator stays on it.
    expect(screen.getByTestId('project-form-step')).toHaveTextContent('step-2');
  });
});
