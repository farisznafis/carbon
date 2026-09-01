import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { ProfileCompanyViewModel, ProfileViewModel } from '@/api/profile';
import { ProfileInfoCards } from './ProfileInfoCards';

const profile: ProfileViewModel = {
  companyId: 1,
  username: 'yamada.taro',
  role: 'user',
  siteName: '東京工場',
  firstName: '太郎',
  lastName: '山田',
  firstNameKana: 'タロウ',
  lastNameKana: 'ヤマダ',
  email: 'yamada@example.com',
  userPosition: '担当者',
  phoneNumber: '09011112222',
  company: null,
};

const company: ProfileCompanyViewModel = {
  phoneNumber: '0311112222',
  nameJA: 'GX株式会社',
  nameFurigana: 'ジーエックスカブシキガイシャ',
  nameEN: 'GX Inc.',
  address: '東京都千代田区1-1-1',
  corpNumber: '1234567890123',
  fax: null,
  email: 'company@example.com',
};

describe('ProfileInfoCards', () => {
  it('uses shared profile sections and keeps password change as the final action', () => {
    const onPasswordChange = vi.fn();
    render(
      <ProfileInfoCards
        profile={profile}
        company={company}
        onPasswordChange={onPasswordChange}
      />,
    );

    expect(screen.getByRole('heading', { name: '個人情報' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '法人情報' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'パスワード変更' })).toBeInTheDocument();
    expect(screen.getByText('山田 太郎')).toBeInTheDocument();
    expect(screen.getByText('GX株式会社')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'パスワードを変更' }));
    expect(onPasswordChange).toHaveBeenCalledOnce();
  });
});
