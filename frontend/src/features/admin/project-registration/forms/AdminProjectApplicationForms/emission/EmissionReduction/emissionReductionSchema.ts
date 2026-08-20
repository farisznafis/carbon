import { ABSOLUTE_EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION } from '@/lib/constants/emission_helpers';
import { z } from 'zod';

/**
 * 1 行 = 1 年度 of 4.1 プログラム型プロジェクトの全体計画.
 *
 * The derived columns are optional here because the server owns them: it
 * rebuilds the 2013–2050 skeleton on every read and every save, so the form
 * only ever carries them, never originates them.
 */
export const emissionReductionYearSchema = z.object({
  // derived
  year: z.number().optional(),
  fiscalYearStart: z.string().optional(),
  fiscalYearEnd: z.string().optional(),
  fiscalYearDays: z.number().optional(),
  certApplicationDays: z.number().optional(),

  // entered
  newMembersTotal: z.number().min(0, '0以上で入力してください'),
  cumulativeMembersTotal: z.number().min(0, '0以上で入力してください'),
  membersTotal: z.number().min(0, '0以上で入力してください').optional(),
  baselineEmissions: z.number().optional(),
  afterImplEmissions: z.number().optional(),

  // derived from the page's 単位
  baselineEmissionsUnit: z.string().optional(),
  afterImplEmissionsUnit: z.string().optional(),
});

export const emissionReductionSchema = z.object({
  // t-CO2 / kg-CO2, not the /年 pair: a row is one 年度's absolute figure and
  // the 合計 below the table sums it.
  unitEmission: z.enum(ABSOLUTE_EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION),

  certTargetPeriodStart: z.coerce.date(),

  certTargetPeriodEnd: z.coerce.date(),

  certTargetPeriodYears: z.number(),

  years: z.array(emissionReductionYearSchema),

  // The 合計 row. Computed server-side from `years`; never typed.
  baselineEmissionsTotal: z.number().optional(),
  afterImplEmissionsUnitTotal: z.number().optional(),
  emissionReductionTotal: z.number().optional(),
  emissionReductionAnnualAverage: z.number().optional(),
});
