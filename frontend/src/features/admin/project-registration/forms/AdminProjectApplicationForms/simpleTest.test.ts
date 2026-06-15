// simpleTest.test.ts
import { describe, it, expect } from 'vitest';
import { z } from 'zod';

// Your schema
const formSchema = z.object({
  projectName: z.string().min(1),
  budget: z.number().positive(),
});

describe('Simple Form Payload Test', () => {
  it('✅ Valid payload', () => {
    const payload = {
      projectName: 'Test Project',
      budget: 1000,
    };

    const result = formSchema.safeParse(payload);
    
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.projectName).toBe('Test Project');
      expect(result.data.budget).toBe(1000);
    }
  });

  it('❌ Invalid payload - empty name', () => {
    const payload = {
      projectName: '',
      budget: 1000,
    };

    const result = formSchema.safeParse(payload);
    
    expect(result.success).toBe(false);
  });

  it('❌ Invalid payload - negative budget', () => {
    const payload = {
      projectName: 'Test',
      budget: -100,
    };

    const result = formSchema.safeParse(payload);
    
    expect(result.success).toBe(false);
  });
});