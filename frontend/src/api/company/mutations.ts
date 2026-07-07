import { apiRequest, apiVoidRequest } from '../_shared/httpClient';
import type {
  Company,
  UpdateCompanyResponse,
  ValidateSignupCompanyInfoPayload,
  ValidateSignupCompanyInfoResponse,
  ValidateSignupMethodologyPayload,
  ValidateSignupMethodologyResponse,
} from './types';

export const updateCompany = async (
  company: Partial<Company>,
  mergedData?: Company,
): Promise<UpdateCompanyResponse> => {
  await apiVoidRequest('/company', {
    method: 'PATCH',
    body: company,
    fallbackErrorMessage: 'Failed to update company',
  });

  return { statusCode: 'OK', company: mergedData ?? (company as Company) };
};

export const validateSignupCompanyInfo = async (
  payload: ValidateSignupCompanyInfoPayload,
): Promise<ValidateSignupCompanyInfoResponse> => {
  return apiRequest<ValidateSignupCompanyInfoResponse>('/company/signup/company-info/validate', {
    method: 'POST',
    auth: false,
    body: payload,
    fallbackErrorMessage: 'Failed to validate signup company info',
  });
};

export const validateSignupMethodology = async (
  payload: ValidateSignupMethodologyPayload,
): Promise<ValidateSignupMethodologyResponse> => {
  return apiRequest<ValidateSignupMethodologyResponse>('/company/signup/methodology/validate', {
    method: 'POST',
    auth: false,
    body: payload,
    fallbackErrorMessage: 'Failed to validate signup methodology',
  });
};
