import { Fragment } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { cn } from '@/lib/utils';

export type PageBreadcrumbItem = {
  label: string;
  /** Omit on the current page; ancestors without a route render as plain text. */
  to?: string;
  /** For views that live inside another page's state rather than at their own route. */
  onClick?: () => void;
};

const crumbLinkClassName = 'inline-flex min-h-6 max-w-60 items-center gap-1';

type PageBreadcrumbProps = {
  /** Ordered from the section root to the current page. */
  items: readonly PageBreadcrumbItem[];
  className?: string;
};

/** Finds the closest linked ancestor, which becomes the only visible item on small screens. */
function findParentIndex(items: readonly PageBreadcrumbItem[]) {
  for (let index = items.length - 2; index >= 0; index -= 1) {
    if (items[index].to || items[index].onClick) return index;
  }
  return -1;
}

/**
 * Shows where the current page sits in the hierarchy and replaces the page back button.
 * Pages declare their trail with `usePageBreadcrumb`; the topbar renders it here.
 *
 * On small screens the trail collapses to a single "‹ parent" link so the header
 * stays one line; every link is rendered once, so there is no duplicate in the DOM.
 */
export function PageBreadcrumb({ items, className }: Readonly<PageBreadcrumbProps>) {
  const parentIndex = findParentIndex(items);
  const isCollapsible = parentIndex >= 0;

  return (
    <Breadcrumb className={className}>
      <BreadcrumbList className="flex-nowrap">
        {items.map((item, index) => {
          const isCurrent = index === items.length - 1;
          const isParent = index === parentIndex;
          const hiddenOnMobile = isCollapsible && !isParent;
          const content = (
            <>
              {isParent && (
                <ChevronLeft aria-hidden="true" className="size-4 shrink-0 sm:hidden" />
              )}
              <span className="truncate">{item.label}</span>
            </>
          );

          return (
            <Fragment key={`${index}-${item.label}`}>
              {index > 0 && (
                <BreadcrumbSeparator className={cn(isCollapsible && 'hidden sm:block')} />
              )}
              <BreadcrumbItem
                className={cn(
                  index === 0 ? 'shrink-0' : 'min-w-0',
                  hiddenOnMobile && 'hidden sm:inline-flex',
                )}
              >
                {isCurrent ? (
                  <BreadcrumbPage className="block truncate" title={item.label}>
                    {item.label}
                  </BreadcrumbPage>
                ) : item.to ? (
                  <BreadcrumbLink asChild>
                    <Link to={item.to} title={item.label} className={crumbLinkClassName}>
                      {content}
                    </Link>
                  </BreadcrumbLink>
                ) : item.onClick ? (
                  <BreadcrumbLink asChild>
                    <button
                      type="button"
                      onClick={item.onClick}
                      title={item.label}
                      className={crumbLinkClassName}
                    >
                      {content}
                    </button>
                  </BreadcrumbLink>
                ) : (
                  <span className="block max-w-60 truncate" title={item.label}>
                    {item.label}
                  </span>
                )}
              </BreadcrumbItem>
            </Fragment>
          );
        })}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
