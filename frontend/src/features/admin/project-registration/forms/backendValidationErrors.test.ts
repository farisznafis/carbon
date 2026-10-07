import { renderHook } from '@testing-library/react';
import { useForm } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import { applyBackendValidationErrors } from './backendValidationErrors';
import type { AdminProjectRegisterFormInputValues } from './projectFormTypes';

describe('applyBackendValidationErrors', () => {
  it('maps exact project array response keys and messages to visible fields', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('Validation failed', {
      status: 400,
      payload: {
        errors: {
          'project[0].picPhoneNumber': '担当者電話番号は必須です。',
          'project[0].postProjectEmission':
            'プロジェクト実施後排出量は必須です。',
        },
      },
    });

    const message = applyBackendValidationErrors(result.current, error);

    // Reported as well as marked inline: the fields it marks belong to step 1,
    // which the operator may not be looking at when the save is rejected.
    expect(message).toBe('Validation failed');
    expect(
      result.current.getFieldState('projectBase.picPhoneNumber').error?.message,
    ).toBe('担当者電話番号は必須です。');
    expect(
      result.current.getFieldState('projectBase.postProjectEmission').error
        ?.message,
    ).toBe('プロジェクト実施後排出量は必須です。');
  });

  it('marks 開始日時 and 利用目的 on the project record', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('入力内容に誤りがあります。', {
      status: 400,
      payload: {
        errors: {
          'project[0].startedAt': '開始日時は必須です。',
          'project[0].usePurpose': '利用目的は必須です。',
        },
      },
    });

    applyBackendValidationErrors(result.current, error);

    expect(
      result.current.getFieldState('projectBase.startedAt').error?.message,
    ).toBe('開始日時は必須です。');
    expect(
      result.current.getFieldState('projectBase.usePurpose').error?.message,
    ).toBe('利用目的は必須です。');
  });

  it('returns the message for a field the form does not have, without inventing one', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('入力内容に誤りがあります。', {
      status: 400,
      payload: { errors: { 'years[3].newMembersTotal': '0以上で入力してください。' } },
    });

    const message = applyBackendValidationErrors(result.current, error);

    expect(message).toBe('入力内容に誤りがあります。');
    expect(result.current.formState.errors).toEqual({});
  });
});
