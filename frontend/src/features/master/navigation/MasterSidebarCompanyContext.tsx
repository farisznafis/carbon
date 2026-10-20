import { Portal as TooltipPortal } from '@radix-ui/react-tooltip';

import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';

type MasterSidebarCompanyContextProps = {
  companyName: string;
  collapsed: boolean;
};

const MASTER_ACCOUNT_LABEL = 'マスターアカウント';

/**
 * Creates compact organization initials for the collapsed sidebar.
 *
 * "EcoTech Solutions Japan K.K." -> "ES"
 *
 * Japanese names fall back to their first two visible characters.
 */
function getOrganizationInitials(companyName: string): string {
  const latinWords =
    companyName.match(/[A-Za-z0-9]+/g) ?? [];

  if (latinWords.length >= 2) {
    return `${latinWords[0][0]}${latinWords[1][0]}`.toUpperCase();
  }

  if (latinWords.length === 1) {
    return latinWords[0]
      .slice(0, 2)
      .toUpperCase();
  }

  return Array.from(
    companyName.replace(/\s/g, ''),
  )
    .slice(0, 2)
    .join('');
}

/**
 * Displays the current company context at the bottom of the Master sidebar.
 *
 * Expanded:
 * EcoTech Solutions Japan K.K.
 * マスターアカウント
 *
 * Collapsed:
 * [ ES ]
 */
export function MasterSidebarCompanyContext({
  companyName,
  collapsed,
}: Readonly<MasterSidebarCompanyContextProps>) {
  const resolvedCompanyName =
    companyName.trim() || '会社名未設定';

  const initials = getOrganizationInitials(
    resolvedCompanyName,
  );

  if (!collapsed) {
    return (
      <div
        className="
          shrink-0
          border-t
          border-sidebar-border
          px-4
          py-4
        "
      >
        <p
          className="
            break-words
            text-sm
            font-medium
            leading-5
            text-sidebar-foreground
          "
        >
          {resolvedCompanyName}
        </p>

        <p
          className="
            mt-1
            text-subtle
            text-muted-foreground
          "
        >
          {MASTER_ACCOUNT_LABEL}
        </p>
      </div>
    );
  }

  return (
    <div
      className="
        flex
        shrink-0
        justify-center
        border-t
        border-sidebar-border
        px-2
        py-4
      "
    >
      <TooltipProvider delayDuration={150}>
        <Tooltip>
          <TooltipTrigger asChild>
            <span
              tabIndex={0}
              aria-label={`${resolvedCompanyName} ${MASTER_ACCOUNT_LABEL}`}
              className={cn(
                'flex h-9 w-9 items-center justify-center',
                'rounded-lg',
                'bg-accent text-accent-foreground',
                'text-xs font-bold',
                'outline-none',
                'focus-visible:ring-2',
                'focus-visible:ring-sidebar-ring',
                'focus-visible:ring-offset-2',
              )}
            >
              {initials}
            </span>
          </TooltipTrigger>

          <TooltipPortal>
            <TooltipContent
              side="right"
              sideOffset={8}
              className="space-y-0.5"
            >
              <p className="font-medium">
                {resolvedCompanyName}
              </p>

              <p className="text-xs">
                {MASTER_ACCOUNT_LABEL}
              </p>
            </TooltipContent>
          </TooltipPortal>
        </Tooltip>
      </TooltipProvider>
    </div>
  );
}