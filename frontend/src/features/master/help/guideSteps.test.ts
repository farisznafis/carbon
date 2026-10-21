import { describe, expect, it } from 'vitest';
import {
  GUIDE_CONTENT,
  getGuideContextForPath,
  type GuideContext,
} from './guideSteps';

const contexts = Object.keys(GUIDE_CONTENT) as GuideContext[];

describe('GUIDE_CONTENT', () => {
  it.each(contexts)('points every %s step at a distinct element', (context) => {
    const targets = GUIDE_CONTENT[context].steps.map((step) => step.target);
    expect(targets.length).toBeGreaterThan(0);
    expect(new Set(targets).size).toBe(targets.length);
  });
});

describe('getGuideContextForPath', () => {
  it.each([
    ['/master/dashboard', 'master-dashboard'],
    ['/dashboard', 'site-dashboard'],
    ['/sites/S1/dashboard', 'site-dashboard'],
    ['/sites/S1/projects/P1', 'project-detail'],
    ['/admin/help-faq', null],
    ['/profile', null],
  ])('maps %s to %s', (pathname, context) => {
    expect(getGuideContextForPath(pathname)).toBe(context);
  });
});

