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
  it.each(contexts)('provides eight steps for %s', (context) => {
    expect(GUIDE_CONTENT[context].steps).toHaveLength(8);
  });

  it('uses page-specific content for each guide context', () => {
    const titles = contexts.map((context) => GUIDE_CONTENT[context].title);
    expect(new Set(titles).size).toBe(contexts.length);
  });
});
