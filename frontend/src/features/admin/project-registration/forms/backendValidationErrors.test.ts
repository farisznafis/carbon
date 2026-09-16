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

describe('applyBackendValidationErrors on a plan page', () => {
  it('marks the page the operator is on, not a same-named project field', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('入力内容に誤りがあります。', {
      status: 400,
      payload: {
        errors: {
          picEmail: '担当者メールアドレスは必須です。',
          emitterCode: '特定排出者コードは必須です。',
        },
      },
    });

    applyBackendValidationErrors(result.current, error, {
      formKey: 'otherProjectOperator',
    });

    // picEmail exists on the project record too. Before the page was named,
    // it landed on projectBase and page 3 showed a banner with nothing marked.
    expect(
      result.current.getFieldState('otherProjectOperator.picEmail').error
        ?.message,
    ).toBe('担当者メールアドレスは必須です。');
    expect(
      result.current.getFieldState('projectBase.picEmail').error,
    ).toBeUndefined();
    expect(
      result.current.getFieldState('otherProjectOperator.emitterCode').error
        ?.message,
    ).toBe('特定排出者コードは必須です。');
  });

  it('translates the backend name back to the form name where they differ', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('入力内容に誤りがあります。', {
      status: 400,
      payload: {
        errors: { energyManagFactoryNo: '必須です。' },
      },
    });

    applyBackendValidationErrors(result.current, error, {
      formKey: 'otherProjectOperator',
      renames: { energyManageFactoryNumber: 'energyManagFactoryNo' },
    });

    expect(
      result.current.getFieldState(
        'otherProjectOperator.energyManageFactoryNumber',
      ).error?.message,
    ).toBe('必須です。');
  });
});

describe('applyBackendValidationErrors on a repeating table', () => {
  it('marks the row the server rejected, not the whole table', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('入力内容に誤りがあります。', {
      status: 400,
      payload: {
        errors: { 'rows[1].symbol': '記号は必須です。' },
      },
    });

    applyBackendValidationErrors(result.current, error, { formKey: 'emissionCalc' });

    // Left as `rows[1]`, setError would invent a field of that literal name;
    // the cell the operator has to fix would never be marked.
    expect(
      result.current.getFieldState('emissionCalc.rows.1.symbol' as never).error
        ?.message,
    ).toBe('記号は必須です。');
    expect(
      result.current.getFieldState('emissionCalc.rows.0.symbol' as never).error,
    ).toBeUndefined();
  });

  it('renames the table and the column independently', () => {
    const { result } = renderHook(() =>
      useForm<AdminProjectRegisterFormInputValues>(),
    );
    const error = new ApiError('入力内容に誤りがあります。', {
      status: 400,
      payload: {
        errors: { 'monitorItems[0].monitorPoint': '必須です。' },
      },
    });

    applyBackendValidationErrors(result.current, error, {
      formKey: 'emissionCalc',
      renames: { items: 'monitorItems', monitoringPoint: 'monitorPoint' },
    });

    expect(
      result.current.getFieldState(
        'emissionCalc.items.0.monitoringPoint' as never,
      ).error?.message,
    ).toBe('必須です。');
  });
});
