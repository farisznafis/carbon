import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FinalTermsStep } from './FinalTermsStep';

const memberTermsProps = {
  projectName: '関東太陽光発電プロジェクト',
  operatorName: '株式会社ABC・山田太郎',
  projectOverview: '地域の太陽光発電設備を集約する事業',
  usePurpose: '地域脱炭素施策',
  startedAt: '2026-09-03T01:05:12.040Z',
};

describe('FinalTermsStep', () => {
  it('uses the shared required consent control', () => {
    render(
      <FinalTermsStep
        {...memberTermsProps}
        formValues={{ finalTermsAgreed: false }}
        onValueChange={vi.fn()}
      />,
    );

    expect(
      screen.getByRole('checkbox', { name: /同意する\s*\*/ }),
    ).toHaveAttribute('aria-required', 'true');
    expect(
      screen.getByRole('heading', {
        name: /「関東太陽光発電プロジェクト」入会規約/,
      }),
    ).toBeVisible();
    expect(
      screen.getByText(
        '本会の名称は、関東太陽光発電プロジェクト (以下「本会」という) とする。',
      ),
    ).toBeVisible();
    expect(screen.getByText(/株式会社ABC・山田太郎が実施する/)).toBeVisible();
    expect(screen.getByText(/「地域の太陽光発電設備を集約する事業」/)).toBeVisible();
    expect(screen.getAllByText(/地域脱炭素施策/).length).toBeGreaterThan(0);
    expect(screen.getByText('本規約は、2026年9月3日から施行する。')).toBeVisible();
    expect(document.body).not.toHaveTextContent('プログラム型運営・管理者名');
    expect(document.body).not.toHaveTextContent('「プロジェクト概要」');
    expect(document.body).not.toHaveTextContent('YYYY年MM月DD');
    expect(document.body).not.toHaveTextContent(
      '※利用目的は、プロジェクトにおける活用予定・実際の活用方法に応じてご修正ください。',
    );
    expect(document.body).toHaveTextContent(
      '※補助金受給者を対象としたプロジェクト等で退会を禁ずる場合や、その他プロジェクトの実際に応じて適宜ご修正ください。',
    );
  });

  it('shows the backend validation message on the agreement checkbox', async () => {
    render(
      <FinalTermsStep
        {...memberTermsProps}
        formValues={{ finalTermsAgreed: false }}
        onValueChange={vi.fn()}
        errors={{ finalTermsAgreed: '入会規約への同意が必要です。' }}
      />,
    );

    expect(
      await screen.findByText('入会規約への同意が必要です。'),
    ).toBeInTheDocument();
  });
});
