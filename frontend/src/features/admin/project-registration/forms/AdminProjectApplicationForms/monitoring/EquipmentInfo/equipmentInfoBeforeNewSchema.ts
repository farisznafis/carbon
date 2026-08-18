import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

/**
 * 設備情報の収集・確認方法（実施前 - 新設）.
 *
 * A new installation has no existing equipment to describe, so this page
 * carries only the standard-equipment statement — unlike the 更新 and
 * 削減活動実施後 pages, which list the equipment itself.
 */
export const equipmentInfoBeforeNewSchema = z.object({
  standardEquipmentSetting: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
});
