/**
 * Compatibility barrel for API types.
 *
 * New code should import domain types from their module, for example
 * `@/api/projects`, `@/api/auth`, or `@/api/monitoring`.
 */

export type {
  Company,
  User,
  UserList,
  UserProfile,
  UserProfileResponse,
} from './users/types';

export type {
  EquipmentItem,
  LoginResponse,
  MethodologyRequest,
  SignUpData,
  SignUpResponse,
} from './auth/types';

export type {
  CompanyProjectRelation,
  Equipment,
  Project,
  ProjectDetail,
  ProjectDetailResponse,
} from './projects/types';

export type {
  CreateSiteData,
  Site,
  SiteAddResponse,
} from './site/types';

export type {
  MonitoringData,
  SubmitDataResponse,
} from './monitoring/types';

export interface ErrorResponse {
  code?: string;
  message: string;
  statusCode?: number;
  details?: Record<string, unknown>;
  field?: string;
}

export interface DeviceData {
  deviceId?: string;
  parameter?: string;
  value?: number;
  unit?: string;
  timestamp?: string;
}
