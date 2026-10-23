import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SignupRevisionPage from './SignupRevisionPage';

const {
  exchangeActionLinkMock,
  getSignupRevisionMock,
  updateSignupRevisionMock,
} = vi.hoisted(() => ({
  exchangeActionLinkMock: vi.fn(),
  getSignupRevisionMock: vi.fn(),
  updateSignupRevisionMock: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  exchangeActionLink: (...args: unknown[]) => exchangeActionLinkMock(...args),
}));

vi.mock('@/api/company', () => ({
  getSignupRevision: (...args: unknown[]) => getSignupRevisionMock(...args),
  updateSignupRevision: (...args: unknown[]) => updateSignupRevisionMock(...args),
}));

vi.mock('@/pages/signup/SignupStep1', () => ({
  SignupStep1: ({
    initialData,
    onNext,
  }: {
    initialData: Record<string, unknown>;
    onNext: (values: Record<string, unknown>) => void;
  }) => (
    <div>
      法人情報フォーム
      <button type="button" onClick={() => onNext(initialData)}>方法論へ続く</button>
    </div>
  ),
}));
vi.mock('@/pages/signup/SignupStep2', () => ({
  SignupStep2: ({
    initialData,
    onNext,
  }: {
    initialData: Record<string, unknown>;
    onNext: (values: Record<string, unknown>) => void;
  }) => (
    <div>
      方法論フォーム
      <button type="button" onClick={() => onNext(initialData)}>修正内容を確認</button>
    </div>
  ),
}));
vi.mock('@/pages/signup/SignupStep3', () => ({
  SignupStep3: ({ onConfirm }: { onConfirm: () => Promise<void> }) => (
    <div>
      確認画面
      <button type="button" onClick={() => void onConfirm()}>修正内容を再申請</button>
    </div>
  ),
}));

const revisionResponse = {
  emailVerificationChallengeId: 'challenge-id',
  corporateNumber: '1234567890123',
  corporateName: '株式会社サンプル',
  corporateNameKana: 'カブシキガイシャサンプル',
  companyNameEnglish: '',
  corporatePhoneNumber: '0312345678',
  postalCode: '1000001',
  prefecture: '東京都',
  municipality: '千代田区',
  blocknum: '1-1',
  buildingName: '',
  lastName: '山田',
  firstName: '太郎',
  lastNameKana: 'ヤマダ',
  firstNameKana: 'タロウ',
  position: '部長',
  phoneNumber: '09012345678',
  faxNumber: '',
  email: 'taro@example.com',
  methodology: ['EN-R-002'],
  activityInformation: '-',
  rejectionReason: '法人番号を確認してください。',
};

describe('SignupRevisionPage', () => {
  beforeEach(() => {
    exchangeActionLinkMock.mockReset();
    getSignupRevisionMock.mockReset();
    updateSignupRevisionMock.mockReset();
    exchangeActionLinkMock.mockResolvedValue({
      statusCode: 'OK',
      actionSessionToken: 'revision-action-session',
      purpose: 'COMPANY_SIGNUP_REVISION',
      expiresInSeconds: 900,
    });
    sessionStorage.clear();
    window.history.replaceState(
      window.history.state,
      '',
      '/signup/revision#token=email-link-token',
    );
  });

  it('reloads the revision with the stored session after a refresh', async () => {
    getSignupRevisionMock.mockResolvedValue(revisionResponse);
    sessionStorage.setItem(
      'action-session:signup-revision',
      JSON.stringify({ actionSessionToken: 'stored-session', expiresAt: Date.now() + 60_000 }),
    );
    window.history.replaceState(window.history.state, '', '/signup/revision');

    render(
      <MemoryRouter initialEntries={['/signup/revision']}>
        <SignupRevisionPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('法人情報フォーム')).toBeInTheDocument();
    expect(exchangeActionLinkMock).not.toHaveBeenCalled();
    expect(getSignupRevisionMock).toHaveBeenCalledWith('stored-session');
  });

  it('shows the invalid state and removes an expired stored session', async () => {
    sessionStorage.setItem(
      'action-session:signup-revision',
      JSON.stringify({ actionSessionToken: 'old-session', expiresAt: Date.now() - 1 }),
    );
    window.history.replaceState(window.history.state, '', '/signup/revision');

    render(
      <MemoryRouter initialEntries={['/signup/revision']}>
        <SignupRevisionPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('リンクが無効または期限切れです')).toBeInTheDocument();
    expect(sessionStorage.getItem('action-session:signup-revision')).toBeNull();
    expect(getSignupRevisionMock).not.toHaveBeenCalled();
  });

  it('reads and clears the fragment once in React StrictMode', async () => {
    getSignupRevisionMock.mockResolvedValue(revisionResponse);

    render(
      <StrictMode>
        <MemoryRouter initialEntries={['/signup/revision']}>
          <SignupRevisionPage />
        </MemoryRouter>
      </StrictMode>,
    );

    expect(await screen.findByText('法人情報フォーム')).toBeInTheDocument();
    expect(screen.getByText('法人番号を確認してください。')).toBeInTheDocument();
    await waitFor(() => expect(exchangeActionLinkMock).toHaveBeenCalledOnce());
    expect(exchangeActionLinkMock).toHaveBeenCalledWith('email-link-token');
    expect(getSignupRevisionMock).toHaveBeenCalledWith(
      'revision-action-session',
    );
    expect(window.location.hash).toBe('');
  });

  it('shows an invalid-link state when the fragment has no token', async () => {
    window.history.replaceState(window.history.state, '', '/signup/revision');

    render(
      <MemoryRouter initialEntries={['/signup/revision']}>
        <SignupRevisionPage />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', {
        name: 'リンクが無効または期限切れです',
      }),
    ).toBeInTheDocument();
    expect(exchangeActionLinkMock).not.toHaveBeenCalled();
    expect(getSignupRevisionMock).not.toHaveBeenCalled();
  });

  it('does not call revision APIs when the one-time link cannot be exchanged', async () => {
    exchangeActionLinkMock.mockRejectedValue(new Error('expired'));

    render(
      <MemoryRouter initialEntries={['/signup/revision']}>
        <SignupRevisionPage />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', {
        name: 'リンクが無効または期限切れです',
      }),
    ).toBeInTheDocument();
    expect(exchangeActionLinkMock).toHaveBeenCalledWith('email-link-token');
    expect(getSignupRevisionMock).not.toHaveBeenCalled();
    expect(updateSignupRevisionMock).not.toHaveBeenCalled();
  });

  it('shows the previous registration when GET omits the optional challenge id', async () => {
    const {
      emailVerificationChallengeId: _challengeId,
      ...responseWithoutChallengeId
    } = revisionResponse;
    getSignupRevisionMock.mockResolvedValue(responseWithoutChallengeId);

    render(
      <MemoryRouter initialEntries={['/signup/revision']}>
        <SignupRevisionPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('法人情報フォーム')).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', {
        name: 'リンクが無効または期限切れです',
      }),
    ).not.toBeInTheDocument();
  });

  it('submits the complete revision body and clears the revision state', async () => {
    const user = userEvent.setup();
    getSignupRevisionMock.mockResolvedValue(revisionResponse);
    updateSignupRevisionMock.mockResolvedValue({ statusCode: 'OK' });

    render(
      <MemoryRouter initialEntries={['/signup/revision']}>
        <SignupRevisionPage />
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole('button', { name: '方法論へ続く' }));
    await user.click(screen.getByRole('button', { name: '修正内容を確認' }));
    await user.click(screen.getByRole('button', { name: '修正内容を再申請' }));

    expect(
      await screen.findByRole('heading', { name: '再申請が完了しました' }),
    ).toBeInTheDocument();
    expect(updateSignupRevisionMock).toHaveBeenCalledWith(
      'revision-action-session',
      expect.objectContaining({
        emailVerificationChallengeId: 'challenge-id',
        corporateName: '株式会社サンプル',
        methodology: ['EN-R-002'],
        activityInformation: '-',
      }),
    );
    expect(updateSignupRevisionMock.mock.calls[0][1]).not.toHaveProperty(
      'rejectionReason',
    );
  });
});
