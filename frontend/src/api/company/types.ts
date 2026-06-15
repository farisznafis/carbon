import type { Company } from '../users';

export type { Company } from '../users';

export type UpdateCompanyResponse = {
  statusCode: string;
  company: Company;
};
