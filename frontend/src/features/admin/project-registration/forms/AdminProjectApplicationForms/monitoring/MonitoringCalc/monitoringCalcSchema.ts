import {
  IS_STANDARD_MONITOR_OPTIONS,
  IS_STANDARD_MONITOR_VALIDATION,
  IS_USE_SAMPLING_OPTIONS,
  IS_USE_SAMPLING_VALIDATION,
} from '@/lib/constants/select_types';
import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const monitoringCalcSchema = z
  .object({
    isStandardizeEmissionAct: z.boolean(),

    actMonitoringItems: z
      .string()
      .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),

    isStandardMonitor: z.enum(
      IS_STANDARD_MONITOR_OPTIONS,
      IS_STANDARD_MONITOR_VALIDATION,
    ),

    otherMethod: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
      .optional(),

    isUseSampling: z.enum(IS_USE_SAMPLING_OPTIONS, IS_USE_SAMPLING_VALIDATION),

    samplingMethod: z
      .string()
      .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500))
      .optional(),

    samplingPurpose: z
      .string()
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
      .optional(),

    collectDataAndMeasurement: z
      .string()
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
      .optional(),

    populationComposition: z
      .string()
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
      .optional(),

    explOfRationality: z
      .string()
      .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000))
      .optional(),

    sampleSizeAndFormula: z
      .string()
      .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000))
      .optional(),

    sampleSizeAndFormulaImg: z.instanceof(File).optional(),

    dataAnalysis: z
      .string()
      .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000))
      .optional(),

    implMethodSched: z
      .string()
      .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000))
      .optional(),

    implMethodSchedImg: z.instanceof(File).optional(),
  })
  .refine(
    (data) => {
      // Check always required fields
      if (
        !data.actMonitoringItems ||
        !data.isStandardMonitor ||
        !data.isUseSampling
      ) {
        return false;
      }
      // Conditional: otherMethod required if isStandardMonitor is specific value
      if (
        data.isStandardMonitor ===
          'その他（以下に確認方法をご記入ください。）' &&
        !data.otherMethod
      ) {
        return false;
      }
      // Conditional: sampling fields required if isUseSampling is not 'サンプリングを利用しない'
      if (data.isUseSampling !== 'サンプリングを利用しない') {
        if (
          !data.samplingMethod ||
          !data.samplingPurpose ||
          !data.collectDataAndMeasurement ||
          !data.populationComposition ||
          !data.explOfRationality ||
          !data.sampleSizeAndFormula ||
          !data.dataAnalysis ||
          !data.implMethodSched
        ) {
          return false;
        }
      }
      return true;
    },
    { message: '必須項目が入力されていません' },
  );
