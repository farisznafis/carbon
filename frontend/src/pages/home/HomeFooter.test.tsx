import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { HomeFooter } from './HomeFooter';

describe('HomeFooter', () => {
  it('links to the contact form instead of listing placeholder contact details', () => {
    render(
      <MemoryRouter>
        <HomeFooter />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'お問い合わせフォーム →' })).toHaveAttribute(
      'href',
      '/contact',
    );
    expect(screen.queryByText('接触')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /@|\+81/ })).not.toBeInTheDocument();
    expect(screen.queryByText('利用規約')).not.toBeInTheDocument();
  });
});
