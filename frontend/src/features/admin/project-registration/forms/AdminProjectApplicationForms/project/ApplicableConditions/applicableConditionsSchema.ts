import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

export const applicableConditionsSchema = z.object({
  methodologyCode: z
    .string()
    .max(15, VALIDATION_MESSAGES.MAX_CHARACTERS(15))
    .optional(),

  methodologyVersion: z
    .string()
    .min(1, '必須項目です')
    .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20)),
  esgPledgeCase: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(10)),
  esgPledgeCaseDate: z
    .coerce.date()
    // required date
    // .string()
    // .min(1, '必須項目です') // ✅ Matches other fields
    // .pipe(
    //   z.coerce.date({
    //     invalid_type_error: '有効な日付を入力してください',
    //   })
    // )
  ,

  businessName: z
    .string()
    .min(1, '必須項目です')
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),
  representativePosition: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),
  representativeName: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),
  regulation: z
    .string()
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
    .optional(),
});

export type applicableConditionsValue = z.infer<
  typeof applicableConditionsSchema
>;
