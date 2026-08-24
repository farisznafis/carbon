import { afterEach, describe, expect, it, vi } from 'vitest';
import { uploadFormDataRequest } from './uploadRequest';

type Listener = (event: ProgressEvent) => void;

class FakeXMLHttpRequest {
  static latest: FakeXMLHttpRequest | null = null;

  status = 200;
  responseText = '{"file":{"url":"https://example.com/file.pdf"}}';
  uploadListeners = new Map<string, Listener>();
  requestListeners = new Map<string, Listener>();
  headers = new Map<string, string>();
  upload = {
    addEventListener: (name: string, listener: Listener) => {
      this.uploadListeners.set(name, listener);
    },
  };

  /** Stores the latest fake request for assertions. */
  constructor() {
    FakeXMLHttpRequest.latest = this;
  }

  /** Accepts the same request initialization signature as XMLHttpRequest. */
  open() {}

  /** Records request headers applied by the upload helper. */
  setRequestHeader(name: string, value: string) {
    this.headers.set(name, value);
  }

  /** Registers request lifecycle listeners. */
  addEventListener(name: string, listener: Listener) {
    this.requestListeners.set(name, listener);
  }

  /** Emits measurable progress followed by a successful response. */
  send() {
    this.uploadListeners.get('progress')?.(
      { lengthComputable: true, loaded: 50, total: 100 } as ProgressEvent,
    );
    this.requestListeners.get('load')?.({} as ProgressEvent);
  }

  /** Emits the abort lifecycle used by AbortSignal. */
  abort() {
    this.requestListeners.get('abort')?.({} as ProgressEvent);
  }
}

describe('uploadFormDataRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    FakeXMLHttpRequest.latest = null;
  });

  it('reports real byte progress and parses the response', async () => {
    vi.stubGlobal('XMLHttpRequest', FakeXMLHttpRequest);
    const onProgress = vi.fn();

    const result = await uploadFormDataRequest<{ file: { url: string } }>(
      '/upload',
      new FormData(),
      { onProgress },
    );

    expect(onProgress).toHaveBeenNthCalledWith(1, null);
    expect(onProgress).toHaveBeenNthCalledWith(2, 50);
    expect(result.file.url).toBe('https://example.com/file.pdf');
    expect(FakeXMLHttpRequest.latest?.headers.has('Content-Type')).toBe(false);
  });

  it('rejects failed uploads with the server message', async () => {
    class FailedXMLHttpRequest extends FakeXMLHttpRequest {
      status = 422;
      responseText = '{"message":"ファイルを確認してください。"}';
    }
    vi.stubGlobal('XMLHttpRequest', FailedXMLHttpRequest);

    await expect(
      uploadFormDataRequest('/upload', new FormData()),
    ).rejects.toThrow('ファイルを確認してください。');
  });
});
