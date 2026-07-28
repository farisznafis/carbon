import { EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION } from '@/lib/constants/emission_helpers';
import { REGEX_MESSAGES, REGEX_PATTERNS } from '@/lib/validations';
import { z } from 'zod';

export const emissionReductionSchema = z.object({
  unitEmission: z.enum(EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION),

  certTargetPeriodStart: z.coerce.date(),

  certTargetPeriodEnd: z.coerce.date(),

  certTargetPeriodYears: z.number(),

  year: z.string().regex(/^\d+$/, '有効な数値を入力してください'),

  newMembersTotal: z.number().min(0, '必須項目です'),

  // calculation
  cumulativeMembersTotal: z.number().min(0, '必須項目です'),

  // calculation
  membersTotal: z.number(),

  // calculation
  baselineEmissions: z.number(),
  baselineEmissionsUnit: z.enum(EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION),

  // calculation
  afterImplEmissions: z.number(),

  afterImplEmissionsUnit: z.enum(EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION),

  baselineEmissionsTotal: z.number(),
  afterImplEmissionsUnitTotal: z.number(),
  emissionReductionTotal: z.number(),
});
