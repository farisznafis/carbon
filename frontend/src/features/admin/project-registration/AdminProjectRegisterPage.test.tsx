import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const adminProjectApiMocks = vi.hoisted(() => ({
  createAdminProject: vi.fn(),
  createAdminProjectPlanApplication: vi.fn(),
  getProjectPlanAutofillByProject: vi.fn(),
  getProjectPlanAutofillDefaults: vi.fn(),
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
});
