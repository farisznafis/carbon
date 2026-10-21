import { describe, expect, it } from 'vitest';
import { usernameSchema } from './validations';

describe('usernameSchema', () => {
  it.each(['yamada_taro', 'taro@example', 'Yamada Taro 01', 'taro😀'])('accepts %s like @ValidUsername', (value) => {
    expect(usernameSchema.safeParse(value).success).toBe(true);
  });

  it.each(['やまだ', 'yamada-taro', 'ya', 'a'.repeat(51)])('rejects %s', (value) => {
    expect(usernameSchema.safeParse(value).success).toBe(false);
  });

  it('trims surrounding spaces before checking the length, as the backend saves it trimmed', () => {
    expect(usernameSchema.parse('  taro  ')).toBe('taro');
  });
});
