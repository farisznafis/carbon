import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PresignedFileLink } from './PresignedFileLink';

const { getSiteProjectEquipmentPresignedUrl } = vi.hoisted(() => ({
  getSiteProjectEquipmentPresignedUrl: vi.fn(),
}));

vi.mock('@/api/site', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/site')>();
  return {
    ...actual,
    getSiteProjectEquipmentPresignedUrl,
  };
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('PresignedFileLink', () => {
  it('resolves the s3Key and opens the presigned URL', async () => {
    const user = userEvent.setup();
    const anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    getSiteProjectEquipmentPresignedUrl.mockResolvedValue(
      'https://storage.example.com/existing-pcs.pdf',
    );

    render(
      <PresignedFileLink
        s3Key="equipment/existing-pcs.pdf"
        label="existing-pcs.pdf"
      />,
    );
    await user.click(
      screen.getByRole('button', { name: 'existing-pcs.pdfを表示' }),
    );

    await waitFor(() => {
      expect(getSiteProjectEquipmentPresignedUrl).toHaveBeenCalledWith(
        'equipment/existing-pcs.pdf',
      );
      expect(anchorClick).toHaveBeenCalledOnce();
    });
  });
});
