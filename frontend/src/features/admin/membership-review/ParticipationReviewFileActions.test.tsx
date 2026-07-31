import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ParticipationReviewFileActions } from './ParticipationReviewFileActions';

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

const file = {
  id: 18,
  fileType: 'solar_contract',
  fileId: 'file-1',
  s3Key: 'tmp/equipments/file-1/solar_contract.pdf',
};

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('ParticipationReviewFileActions', () => {
  it('opens the presigned URL in a new tab', async () => {
    const user = userEvent.setup();
    const anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    getSiteProjectEquipmentPresignedUrl.mockResolvedValue(
      'https://storage.example.com/solar_contract.pdf',
    );

    render(<ParticipationReviewFileActions file={file} />);

    await user.click(
      screen.getByRole('button', { name: 'solar_contractを表示' }),
    );

    await waitFor(() => {
      expect(getSiteProjectEquipmentPresignedUrl).toHaveBeenCalledWith(
        file.s3Key,
      );
      expect(anchorClick).toHaveBeenCalledTimes(1);
    });
  });

  it('downloads the presigned file as a blob', async () => {
    const user = userEvent.setup();
    const anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    const createObjectURL = vi.fn().mockReturnValue('blob:equipment-file');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(new Blob(['pdf']), {
          status: 200,
          headers: { 'Content-Type': 'application/pdf' },
        }),
      ),
    );
    getSiteProjectEquipmentPresignedUrl.mockResolvedValue(
      'https://storage.example.com/solar_contract.pdf',
    );

    render(<ParticipationReviewFileActions file={file} />);

    await user.click(
      screen.getByRole('button', {
        name: 'solar_contractをダウンロード',
      }),
    );

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        'https://storage.example.com/solar_contract.pdf',
      );
      expect(createObjectURL).toHaveBeenCalled();
      expect(anchorClick).toHaveBeenCalledTimes(1);
    });
  });
});
