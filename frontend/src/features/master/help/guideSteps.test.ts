import { beforeEach, describe, expect, it } from 'vitest';
import {
  GUIDE_CONTENT,
  getGuideContextForPath,
  type GuideContext,
} from './guideSteps';
import { hasSeenGuideTour, markGuideTourSeen } from './guideTourStorage';

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

describe('guideTourStorage', () => {
  beforeEach(() => localStorage.clear());

  it('remembers a seen tour per account and page', () => {
    markGuideTourSeen('user-1', 'site-dashboard');

    expect(hasSeenGuideTour('user-1', 'site-dashboard')).toBe(true);
    expect(hasSeenGuideTour('user-1', 'project-detail')).toBe(false);
    expect(hasSeenGuideTour('user-2', 'site-dashboard')).toBe(false);
  });

  it('treats unreadable storage as not seen', () => {
    localStorage.setItem('guideToursSeen', '{broken');
    expect(hasSeenGuideTour('user-1', 'site-dashboard')).toBe(false);
  });
});
