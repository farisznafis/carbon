import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Hero, { HeroMediaFrame } from './Hero';

vi.mock('@/hooks/useRedirectIfLoggedIn', () => ({
  useRedirectIfLoggedIn: vi.fn(),
}));

/** Renders the public homepage with a real router for navigation assertions. */
function renderHero() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<Hero />} />
        <Route path="/signup" element={<div>signup destination</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('Hero', () => {
  it('renders the complete Figma homepage with product-accurate Japanese copy', () => {
    renderHero();

    expect(
      screen.getByRole('heading', {
        name: 'カーボンデータを「意思決定」に変えるプラットフォーム',
      }),
    ).toBeVisible();
    expect(
      screen.getByRole('heading', { name: 'このサービスでできること' }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'ご利用の流れ' })).toBeVisible();
    expect(screen.getByRole('heading', { name: '初期対応方法論' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'よくあるご質問' })).toBeVisible();
    expect(screen.getByText('EN-R-002')).toBeVisible();
    expect(screen.getByText('EN-S-006')).toBeVisible();
    expect(screen.queryByText('OTHER')).not.toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: 'GXダッシュボードのプレビュー' }),
    ).toHaveAttribute('src', '/assets/hero/dashboard-preview.png');
    expect(
      screen.getAllByRole('button', { name: 'お問い合わせ（準備中）' }),
    ).toHaveLength(2);
  });

  it.each(['無料で始める', '今すぐ登録する'])(
    'navigates to signup from %s',
    async (linkLabel) => {
      const user = userEvent.setup();
      renderHero();

      await user.click(screen.getByRole('link', { name: linkLabel }));

      expect(await screen.findByText('signup destination')).toBeVisible();
    },
  );

  it('uses accessible internal anchors and expands FAQ content', async () => {
    const user = userEvent.setup();
    renderHero();

    expect(screen.getByRole('link', { name: '特徴' })).toHaveAttribute(
      'href',
      '#features',
    );
    expect(
      screen.getByText(/マスターユーザーは企業に属する複数の拠点を登録/),
    ).toBeVisible();

    await user.click(
      screen.getByRole('button', {
        name: 'J-クレジット制度に詳しくなくても利用できますか？',
      }),
    );

    expect(
      screen.getByText(/画面の案内に沿って、企業・拠点・設備情報の登録/),
    ).toBeVisible();
  });
});

describe('HeroMediaFrame', () => {
  it('renders GIF-compatible image media accessibly', () => {
    render(
      <HeroMediaFrame
        source={{
          kind: 'image',
          src: '/assets/hero/preview.gif',
          alt: 'GIF preview',
        }}
      />,
    );

    expect(screen.getByRole('img', { name: 'GIF preview' })).toHaveAttribute(
      'src',
      '/assets/hero/preview.gif',
    );
  });

  it('renders looping muted inline video media with an optional poster', () => {
    render(
      <HeroMediaFrame
        source={{
          kind: 'video',
          src: '/assets/hero/preview.mp4',
          label: 'Video preview',
          poster: '/assets/hero/poster.png',
        }}
      />,
    );

    const video = screen.getByTitle('Video preview');
    expect(video).toHaveAttribute('src', '/assets/hero/preview.mp4');
    expect(video).toHaveAttribute('poster', '/assets/hero/poster.png');
    expect(video).toHaveAttribute('autoplay');
    expect(video).toHaveAttribute('loop');
    expect(video).toHaveAttribute('playsinline');
    expect(video).toHaveProperty('muted', true);
  });
});
