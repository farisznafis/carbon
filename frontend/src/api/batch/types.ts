export type ReductionResultApiItem = {
  processing_date?: string | null;
  created_at?: string | null;
  empj?: number | string | null;
};

export type ReductionResultPayload =
  | ReductionResultApiItem
  | ReductionResultApiItem[]
  | null;

export type ReductionResultResponse = {
  message?: string | null;
  data?:
    | {
        results?: ReductionResultPayload;
        items?: ReductionResultPayload;
      }
    | ReductionResultPayload;
  results?: ReductionResultPayload;
  items?: ReductionResultPayload;
};

export type EmissionTrendPoint = {
  x: string;
  y: number;
};
