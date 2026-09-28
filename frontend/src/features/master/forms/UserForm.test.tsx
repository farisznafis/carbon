import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
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

/** Renders the shared user form in the operator-registration configuration. */
function renderOperatorUserForm(onNext: (values: UserFormData) => Promise<void>) {
  return render(
    <UserForm
      mode="create"
      defaultValues={EMPTY_USER_FORM}
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

describe('UserForm retry', () => {
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
    renderOperatorUserForm(onNext);

    await user.type(screen.getByPlaceholderText('山田'), '山田');
    await user.click(screen.getByRole('button', { name: '登録する' }));

    expect(await screen.findByText('姓を確認してください。')).toBeInTheDocument();
    expect(onNext).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: '登録する' }));

    await waitFor(() => expect(onNext).toHaveBeenCalledTimes(2));
  });
});
