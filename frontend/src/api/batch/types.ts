export type ReductionResultApiItem = {
  processing_date?: string | null;
  created_at?: string | null;
  empj?: number | string | null;
  empj_m?: number | string | null;
  empj_s_control?: number | string | null;
  empj_s_battery?: number | string | null;
};

export type ReductionResultPayload =
  | ReductionResultApiItem
  | ReductionResultApiItem[]
  | null;

export type ReductionResultResponse = {
  message?: string | null;
  data?:
    | {
        result?: ReductionResultPayload;
        results?: ReductionResultPayload;
        items?: ReductionResultPayload;
      }
    | ReductionResultPayload;
  result?: ReductionResultPayload;
  results?: ReductionResultPayload;
  items?: ReductionResultPayload;
};

export type EmissionTrendPoint = {
  x: string;
  y: number;
};

export type ProjectEmissionBreakdown = {
  mainEmission: number | null;
  controlDeviceEmission: number | null;
  batteryEmission: number | null;
};

export type ProjectEmissionChartData = {
  trend: EmissionTrendPoint[];
  breakdown: ProjectEmissionBreakdown | null;
};
