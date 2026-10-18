import { http } from 'msw';
import { ok } from '../utils';
import { adminHandlers } from './admin';
import { authHandlers } from './auth';
import { fileHandlers } from './files';
import { monitoringHandlers } from './monitoring';
import { postalCodeHandlers } from './postalCode';
import { projectHandlers } from './projects';
import { siteHandlers } from './sites';

export const handlers = [
  ...authHandlers,
  ...siteHandlers,
  ...projectHandlers,
  ...monitoringHandlers,
  ...adminHandlers,
  ...fileHandlers,
  ...postalCodeHandlers,

  // Anything not mocked yet still succeeds, so a new endpoint never breaks the demo.
  http.all('/api/*', ({ request }) => {
    console.warn(`[mock] No handler for ${request.method} ${new URL(request.url).pathname}; returning empty OK.`);
    return ok({});
  }),
];
