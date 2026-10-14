export type ReductionResultApiItem = {
  processing_date?: string | null;
  created_at?: string | null;
  embl?: number | string | null;
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

/** One processing record with its EmPJ components (tCO₂); missing values stay null. */
export type ReductionResultItem = {
  processingDate: string;
  empj: number | null;
  empjM: number | null;
  empjSControl: number | null;
  empjSBattery: number | null;
};

export type EmissionTrendPoint = {
  x: string;
  y: number;
};

/** One processing period of a project's emission history (tCO₂). */
export type ProjectEmissionHistoryPoint = {
  period: string;
  baselineEmission: number | null;
  projectEmission: number | null;
  reduction: number | null;
};

/** One yearly entry of GET /project/{projectId}/monitoring → detail.chart.chartData. */
export type ProjectMonitoringChartItem = {
  year?: string | null;
  baseline?: number | null;
  actualEmission?: number | null;
  reduction?: number | null;
};

export type ProjectMonitoringResponse = {
  statusCode?: string;
  message?: string | null;
  detail?: {
    chart?: {
      chartData?: ProjectMonitoringChartItem[] | null;
    } | null;
  } | null;
};

export type ProjectEmissionBreakdown = {
  mainEmission: number | null;
  controlDeviceEmission: number | null;
  batteryEmission: number | null;
};

export type ProjectReductionResultResponse = {
  statusCode?: string;
  message?: string | null;
  data?: ReductionResultApiItem | null;
};

export type ProjectEmissionSummary = {
  baselineEmission: number | null;
  projectEmission: number | null;
  breakdown: ProjectEmissionBreakdown | null;
};

export type BatchHistoryItem = {
  id: number;
  status: string;
  is_active: boolean;
  created_at: string;
  created_by: string;
  updated_at: string;
  updated_by: string;
  start_time: string;
  end_time: string | null;
  claimed_by: string | null;
  claimed_at: string | null;
};

export type BatchHistoryPagination = {
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
  nextPageToken: string | null;
  hasMore: boolean;
};

export type BatchHistoryResponse = {
  items?: BatchHistoryItem[] | null;
  pagination?: Partial<BatchHistoryPagination> | null;
};

export type BatchHistoryResult = {
  items: BatchHistoryItem[];
  pagination: BatchHistoryPagination;
};
