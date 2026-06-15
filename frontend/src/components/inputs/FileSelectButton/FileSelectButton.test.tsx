// FileSelectButton.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FileSelectButton } from './FileSelectButton';

function createFile(name = 'test.txt') {
  return new File(['content'], name, { type: 'text/plain' });
}

describe('FileSelectButton', () => {
  it('renders label text', () => {
    render(
      <FileSelectButton
        id="file"
        value={null}
        onChange={() => {}}
      />
    );
    expect(screen.getByText('ファイルを選択')).toBeInTheDocument();
  });

  it('renders custom label', () => {
    render(
      <FileSelectButton
        id="file"
        value={null}
        onChange={() => {}}
        label="アップロード"
      />
    );
    expect(screen.getByText('アップロード')).toBeInTheDocument();
  });

  it('calls onChange with selected file', () => {
    const handleChange = vi.fn();
    render(
      <FileSelectButton
        id="file"
        value={null}
        onChange={handleChange}
      />
    );

    const input = screen.getByLabelText('ファイルを選択', { selector: 'input' });
    const file = createFile();

    fireEvent.change(input, {
      target: { files: [file] },
    });

    expect(handleChange).toHaveBeenCalledTimes(1);
    expect(handleChange.mock.calls[0][0]).toBe(file);
  });

  it('shows selected file name', () => {
    const file = createFile('myfile.txt');
    render(
      <FileSelectButton
        id="file"
        value={file}
        onChange={() => {}}
      />
    );
    expect(screen.getByText('myfile.txt')).toBeInTheDocument();
  });
});