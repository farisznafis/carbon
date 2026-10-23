import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import type { UserFormData } from '@/api/users';
import { UserForm } from './UserForm';

const EMPTY_USER_FORM: UserFormData = {
  username: '',
  lastName: '',
  firstName: '',
  lastNameKana: '',
  firstNameKana: '',
  phoneNumber: '',
  email: '',
  password: '',
};

const FILLED_USER_FORM: UserFormData = {
  username: 'yamada_taro',
  lastName: '山田',
  firstName: '太郎',
  lastNameKana: 'ヤマダ',
  firstNameKana: 'タロウ',
  phoneNumber: '0312345678',
  email: 'yamada.taro@example.com',
  password: 'Passw0rd!',
};

/** Renders the shared user form in the operator-registration configuration. */
function renderOperatorUserForm(
  onNext: (values: UserFormData) => Promise<void>,
  defaultValues: UserFormData = EMPTY_USER_FORM,
) {
  return render(
    <UserForm
      mode="create"
      defaultValues={defaultValues}
      originalData={EMPTY_USER_FORM}
      onChange={() => undefined}
      onNext={onNext}
      onBack={() => undefined}
      hideSiteSection
      hidePositionField
      submitLabel="登録する"
      embedded
    />,
  );
}

beforeAll(() => {
  Element.prototype.scrollIntoView = vi.fn();
});

describe('UserForm', () => {
  it('lets an empty form be submitted and lists what is missing above the button', async () => {
    const user = userEvent.setup();
    const onNext = vi.fn<(values: UserFormData) => Promise<void>>();
    renderOperatorUserForm(onNext);

    await user.click(screen.getByRole('button', { name: '登録する' }));

    expect(await screen.findByText('入力内容を確認してください（8件）')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '姓：名前（姓）を入力してください。' })).toBeInTheDocument();
    expect(onNext).not.toHaveBeenCalled();
  });

  it('submits again after a backend field error', async () => {
    const user = userEvent.setup();
    const onNext = vi
      .fn<(values: UserFormData) => Promise<void>>()
      .mockRejectedValueOnce(
        new ApiError('入力内容に不備があります。', {
          status: 400,
          payload: {
            statusCode: 'VALIDATION_ERROR',
            message: '入力内容に不備があります。',
            errors: { lastName: '姓を確認してください。' },
          },
        }),
      )
      .mockResolvedValueOnce(undefined);
    renderOperatorUserForm(onNext, FILLED_USER_FORM);
    // UserForm never pre-fills a password.
    await user.type(screen.getByPlaceholderText('パスワードを入力'), 'Passw0rd!');

    await user.click(screen.getByRole('button', { name: '登録する' }));

    expect(await screen.findByRole('button', { name: '姓：姓を確認してください。' })).toBeInTheDocument();
    expect(onNext).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: '登録する' }));

    await waitFor(() => expect(onNext).toHaveBeenCalledTimes(2));
  });
});
