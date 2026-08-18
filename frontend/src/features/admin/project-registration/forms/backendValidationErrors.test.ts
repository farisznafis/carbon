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
});
