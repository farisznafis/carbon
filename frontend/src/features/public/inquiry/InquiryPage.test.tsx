import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import InquiryPage from './InquiryPage';

const { createInquiryMock, toastMock } = vi.hoisted(() => ({
  createInquiryMock: vi.fn(),
  toastMock: vi.fn(),
}));

vi.mock('@/api/notifications', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/notifications')>()),
  createInquiry: createInquiryMock,
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: toastMock }),
}));

/** Renders the public inquiry route and a visible homepage destination. */
function renderInquiryPage() {
  return render(
    <MemoryRouter initialEntries={['/contact']}>
      <Routes>
        <Route path="/contact" element={<InquiryPage />} />
        <Route path="/" element={<div>homepage destination</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('InquiryPage', () => {
  beforeEach(() => {
    createInquiryMock.mockReset();
    toastMock.mockReset();
  });

  it('renders the Figma inquiry layout with accessible required fields', () => {
    renderInquiryPage();

    expect(
      screen.getByRole('heading', { name: 'お問い合わせ' }),
    ).toBeVisible();
    expect(
      screen.getByText(/通常、3営業日以内に担当者よりご返信いたします。/),
    ).toBeVisible();
    expect(screen.getByRole('presentation')).toHaveAttribute(
      'src',
      '/assets/contact/inquiry-guide.png',
    );

    ['メールアドレス', '会社名', 'お名前', 'お問い合わせ内容'].forEach(
      (label) => {
        const control = screen.getByRole('textbox', { name: new RegExp(label) });
        expect(control).toHaveAttribute('aria-required', 'true');
        expect(control).not.toHaveAttribute('required');
      },
    );
  });

  it('submits empty raw values without frontend validation', async () => {
    const user = userEvent.setup();
    createInquiryMock.mockResolvedValue({
      statusCode: 'OK',
      message: 'お問い合わせを受け付けました。',
    });
    renderInquiryPage();

    await user.click(screen.getByRole('button', { name: '送信する' }));

    expect(createInquiryMock).toHaveBeenCalledWith({
      email: '',
      companyName: '',
      name: '',
      content: '',
    });
    expect(
      await screen.findByRole('heading', {
        name: 'お問い合わせを受け付けました',
      }),
    ).toBeVisible();
    expect(screen.getByText('お問い合わせを受け付けました。')).toBeVisible();
  });

  it('maps prefixed backend errors and preserves entered values', async () => {
    const user = userEvent.setup();
    createInquiryMock.mockRejectedValue(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            'request::email': 'メールアドレスを確認してください。',
            content: 'お問い合わせ内容を入力してください。',
          },
        },
      }),
    );
    renderInquiryPage();

    const emailInput = screen.getByRole('textbox', {
      name: /メールアドレス/,
    });
    const contentInput = screen.getByRole('textbox', {
      name: /お問い合わせ内容/,
    });
    await user.type(emailInput, 'invalid-email');
    await user.type(contentInput, '相談内容');
    await user.click(screen.getByRole('button', { name: '送信する' }));

    expect(
      await screen.findByText('メールアドレスを確認してください。'),
    ).toBeVisible();
    expect(
      screen.getByText('お問い合わせ内容を入力してください。'),
    ).toBeVisible();
    expect(emailInput).toHaveValue('invalid-email');
    expect(contentInput).toHaveValue('相談内容');
    expect(toastMock).not.toHaveBeenCalled();
  });

  it('disables duplicate submission while the request is pending', async () => {
    const user = userEvent.setup();
    createInquiryMock.mockReturnValue(new Promise(() => undefined));
    renderInquiryPage();

    const submitButton = screen.getByRole('button', { name: '送信する' });
    await user.click(submitButton);

    expect(createInquiryMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: '送信中...' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: '送信中...' }));
    expect(createInquiryMock).toHaveBeenCalledTimes(1);
  });

  it('keeps the form values and shows a toast for a general API error', async () => {
    const user = userEvent.setup();
    createInquiryMock.mockRejectedValue(new Error('送信サービスを利用できません。'));
    renderInquiryPage();

    const companyInput = screen.getByRole('textbox', { name: /会社名/ });
    await user.type(companyInput, '株式会社ABC');
    await user.click(screen.getByRole('button', { name: '送信する' }));

    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({
        description: '送信サービスを利用できません。',
        variant: 'destructive',
      }),
    );
    expect(companyInput).toHaveValue('株式会社ABC');
  });

  it('shows success briefly and returns home after three seconds', async () => {
    let redirectCallback: (() => void) | undefined;
    const setTimeoutSpy = vi
      .spyOn(window, 'setTimeout')
      .mockImplementation((callback) => {
        redirectCallback = callback as () => void;
        return 1;
      });
    createInquiryMock.mockResolvedValue({ statusCode: 'OK', message: '' });
    renderInquiryPage();

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: '送信する' }));
    });

    expect(
      screen.getByRole('heading', { name: 'お問い合わせを受け付けました' }),
    ).toBeVisible();
    expect(screen.getByText('お問い合わせありがとうございます。')).toBeVisible();
    expect(screen.getByText('3秒後にホームへ戻ります。')).toBeVisible();
    expect(screen.getByRole('button', { name: 'ホームへ戻る' })).toBeVisible();
    expect(setTimeoutSpy).toHaveBeenCalledWith(expect.any(Function), 3_000);

    act(() => redirectCallback?.());
    expect(screen.getByText('homepage destination')).toBeVisible();

    setTimeoutSpy.mockRestore();
  });
});
