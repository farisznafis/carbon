import type { CompanyStatus } from '@/api/admin';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { COMPANY_STATUS_CONTENT } from './companyStatusContent';

/** Shows a company status in its shared label and tone. */
export function CompanyStatusBadge({
  status,
  className,
}: Readonly<{ status: CompanyStatus; className?: string }>) {
  const content = COMPANY_STATUS_CONTENT[status];

  return (
    <StatusBadge tone={content.tone} className={className}>
      {content.label}
    </StatusBadge>
  );
}
