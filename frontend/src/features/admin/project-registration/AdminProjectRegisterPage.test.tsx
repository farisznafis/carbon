import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const adminProjectApiMocks = vi.hoisted(() => ({
  createAdminProject: vi.fn(),
  getAdminProjectDetail: vi.fn(),
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
    adminProjectApiMocks.getAdminProjectDetail.mockResolvedValue({
      id: 'project-9',
      projectName: '既存プロジェクト',
      projectNumber: 'P9',
      projectStatus: '準備中',
      repLastName: '山田',
    });
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

  it('resumes an existing project at its first unfinished page', async () => {
    adminProjectApiMocks.getProjectPlanProgress.mockResolvedValue({
      totalForms: 33,
      completedForms: 2,
      nextIncompleteSlug: 'applicable-conditions',
      forms: [],
    });

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/',
            state: {
              project: {
                id: 'project-9',
                projectName: '既存プロジェクト',
                projectStatus: '準備中',
              },
            },
          },
        ]}
      >
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    // applicable-conditions is step 4.
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-4',
    );
    expect(adminProjectApiMocks.getProjectPlanForm).toHaveBeenCalledWith(
      'project-9',
      'applicable-conditions',
    );
    // The whole point: resuming must not create a second project.
    expect(adminProjectApiMocks.createAdminProject).not.toHaveBeenCalled();
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
  it('loads every page as the operator arrives on it', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: '次へ' }));
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-2',
    );
    await waitFor(() => {
      expect(adminProjectApiMocks.getProjectPlanForm).toHaveBeenCalledWith(
        'project-1',
        'application',
      );
    });

    await user.click(screen.getByRole('button', { name: '次へ' }));
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-3',
    );

    // Arriving on a page has to fetch it. Otherwise the operator sees the
    // previous page's read-only and 有/無 rules, its master-data fields come up
    // empty, and the save is then rejected — the page becomes a dead end.
    await waitFor(() => {
      expect(adminProjectApiMocks.getProjectPlanForm).toHaveBeenCalledWith(
        'project-1',
        'other-operator-projects',
      );
    });
  });

  it('opens the page the caller asked for', async () => {
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/',
            state: {
              project: {
                id: 'project-9',
                projectName: '既存プロジェクト',
                projectStatus: '準備中',
              },
              planSlug: 'monitoring-plan',
            },
          },
        ]}
      >
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    // monitoring-plan is step 32, and it wins over the resume position.
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-32',
    );
  });

  it('opens a finished plan on its first page so any page can be updated', async () => {
    adminProjectApiMocks.getProjectPlanProgress.mockResolvedValue({
      totalForms: 33,
      completedForms: 33,
      nextIncompleteSlug: null,
      forms: [],
    });

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/',
            state: {
              project: {
                id: 'project-9',
                projectName: '完了プロジェクト',
                projectStatus: '準備中',
              },
            },
          },
        ]}
      >
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    // Nothing is unfinished, so there is no page to resume at. Landing on the
    // first plan page — not stranded on step 1 — is what makes 計画書更新 work.
    expect(await screen.findByTestId('project-form-step')).toHaveTextContent(
      'step-2',
    );
    expect(adminProjectApiMocks.createAdminProject).not.toHaveBeenCalled();
  });
  it('fills step 1 from the project record, not the list row it was opened with', async () => {
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/',
            state: {
              project: {
                id: 'project-9',
                projectName: '既存プロジェクト',
                projectStatus: '準備中',
              },
              planSlug: 'applicable-conditions',
            },
          },
        ]}
      >
        <AdminProjectRegister />
      </MemoryRouter>,
    );

    // The row carries a handful of fields; step 1 has far more, and all of
    // them are required. Reading only the row left it blank and the first
    // save was rejected field by field with nothing shown on screen.
    await waitFor(() =>
      expect(adminProjectApiMocks.getAdminProjectDetail).toHaveBeenCalledWith(
        'project-9',
      ),
    );
  });
});
