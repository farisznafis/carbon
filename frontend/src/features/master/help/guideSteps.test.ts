import { describe, expect, it } from 'vitest';
import {
  GUIDE_CONTENT,
  type GuideContext,
} from './guideSteps';

const contexts: GuideContext[] = [
  'master-dashboard',
  'site-dashboard',
  'project-detail',
];

describe('GUIDE_CONTENT', () => {
  it.each([
    ['master-dashboard', 8],
    ['site-dashboard', 5],
    ['project-detail', 6],
  ] satisfies Array<[GuideContext, number]>)(
    'provides the configured number of steps for %s',
    (context, totalSteps) => {
      expect(GUIDE_CONTENT[context].steps).toHaveLength(totalSteps);
    },
  );

  it('uses page-specific content for each guide context', () => {
    const titles = contexts.map((context) => GUIDE_CONTENT[context].title);
    expect(new Set(titles).size).toBe(contexts.length);
  });
});
