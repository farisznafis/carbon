import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import {
  AppLoadingScreen,
  FileUploadProgress,
  PageLoadingOverlay,
  PageSkeleton,
  ProcessingProgress,
  SkeletonTable,
} from './index';

describe('shared loading experience', () => {
  it('renders the branded defaults as an accessible application status', () => {
    render(<AppLoadingScreen />);

    const status = screen.getByRole('status');
    expect(status).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('データを読み込んでいます')).toBeVisible();
    expect(screen.getByText('少々お待ちください')).toBeVisible();
    expect(status.querySelector('.motion-reduce\\:animate-none')).toBeInTheDocument();
  });

  it('supports context-specific loading copy', () => {
    render(
      <AppLoadingScreen
        message="ダッシュボードを読み込んでいます"
        secondaryMessage="準備が整うまでお待ちください"
      />,
    );

    expect(screen.getByText('ダッシュボードを読み込んでいます')).toBeVisible();
    expect(screen.getByText('準備が整うまでお待ちください')).toBeVisible();
  });

  it('blocks page interaction while showing the branded page animation', () => {
    render(<PageLoadingOverlay message="ダッシュボードを読み込んでいます" />);

    const status = screen.getByRole('status');
    expect(status).toHaveAttribute('aria-disabled', 'true');
    expect(status).toHaveAttribute('data-page-interactions-disabled', 'true');
    expect(screen.getByText('ダッシュボードを読み込んでいます')).toBeVisible();
  });

  it('routes legacy table skeleton calls to one animated loading panel', () => {
    const { container } = render(<SkeletonTable columns={4} rows={12} />);

    expect(screen.getByRole('status', { name: '一覧を読み込んでいます' })).toBeVisible();
    expect(container.querySelector('.loading-orbit')).toBeInTheDocument();
    expect(container.querySelectorAll('[class*="animate-pulse"]')).toHaveLength(0);
  });

  it('routes legacy page skeleton calls to the branded page animation', () => {
    const { container } = render(<PageSkeleton />);
    expect(screen.getByRole('status')).toHaveTextContent('ページを読み込んでいます');
    expect(container.querySelector('.loading-orbit')).toBeInTheDocument();
    expect(container.querySelectorAll('[class*="animate-pulse"]')).toHaveLength(0);
  });

  it('renders measured and failed file states with a targeted retry action', () => {
    const onRetry = vi.fn();
    render(
      <FileUploadProgress
        items={[
          { id: 'uploading', fileName: '発電量.pdf', status: 'uploading', progress: 68 },
          { id: 'failed', fileName: '仕様書.pdf', status: 'error', progress: null },
        ]}
        onRetry={onRetry}
      />,
    );

    expect(screen.getByText('アップロード中... 68%')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: '再試行' }));
    expect(onRetry).toHaveBeenCalledWith('failed');
  });

  it('uses indeterminate processing when no honest percentage is available', () => {
    const { container } = render(
      <ProcessingProgress
        title="モニタリング報告書を作成しています"
        stages={[
          { id: 'check', label: 'データを確認しています', status: 'complete' },
          { id: 'report', label: '報告書を生成しています', status: 'active' },
        ]}
      />,
    );

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true');
    expect(container.querySelector('.loading-progress-indeterminate')).toBeInTheDocument();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });
});
