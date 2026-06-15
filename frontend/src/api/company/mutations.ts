import { apiVoidRequest } from '../_shared/httpClient';
import type { Company, UpdateCompanyResponse } from './types';

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
