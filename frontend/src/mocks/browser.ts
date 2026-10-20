import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);

/** Starts the mock API; every `/api/*` request is answered in the browser instead of by the backend. */
export async function startMockApi() {
  await worker.start({
    serviceWorker: { url: `${import.meta.env.BASE_URL}mockServiceWorker.js` },
    onUnhandledRequest: 'bypass',
    quiet: true,
  });

  // A tab coming back from the background may be talking to a restarted worker.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') void reactivateMockApi();
  });
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) void reactivateMockApi();
  });
}

/**
 * Re-registers this tab with the mock service worker.
 *
 * The browser stops an idle service worker and starts a fresh one on the next
 * event; the fresh one has forgotten which tabs enabled mocking, so it lets
 * `/api/*` through to the static host (which answers 405 or index.html).
 * Sending MOCK_ACTIVATE again restores it without reloading the page.
 */
export async function reactivateMockApi(): Promise<void> {
  if (!('serviceWorker' in navigator)) return;

  const registration = await navigator.serviceWorker.getRegistration(import.meta.env.BASE_URL);
  const target = navigator.serviceWorker.controller ?? registration?.active;
  if (!target) return;

  await new Promise<void>((resolve) => {
    const timeout = window.setTimeout(done, 1000);
    function done() {
      window.clearTimeout(timeout);
      navigator.serviceWorker.removeEventListener('message', onMessage);
      resolve();
    }
    function onMessage(event: MessageEvent) {
      if (event.data?.type === 'MOCKING_ENABLED') done();
    }
    navigator.serviceWorker.addEventListener('message', onMessage);
    target.postMessage('MOCK_ACTIVATE');
  });
}
