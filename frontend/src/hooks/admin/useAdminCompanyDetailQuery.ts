import { useQuery } from '@tanstack/react-query';
import { getAdminCompanyDetail } from '@/api/admin';

export const adminCompanyDetailQueryKey = (companyId: string) =>
  ['admin', 'companies', companyId] as const;

export function useAdminCompanyDetailQuery(companyId?: string) {
  return useQuery({
    queryKey: adminCompanyDetailQueryKey(companyId ?? ''),
    queryFn: () => getAdminCompanyDetail(companyId as string),
    enabled: Boolean(companyId),
    staleTime: 30_000,
  });
}
