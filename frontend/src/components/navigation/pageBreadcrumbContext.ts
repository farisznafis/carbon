import {
  createContext,
  useContext,
  useLayoutEffect,
  useRef,
  type Dispatch,
  type SetStateAction,
} from 'react';

import type { PageBreadcrumbItem } from './PageBreadcrumb';

export type BreadcrumbItems = readonly PageBreadcrumbItem[];

export const EMPTY_BREADCRUMB_ITEMS: BreadcrumbItems = [];

export const BreadcrumbItemsContext = createContext<BreadcrumbItems>(EMPTY_BREADCRUMB_ITEMS);
// Split from the items so declaring pages do not re-render when the trail changes.
export const SetBreadcrumbItemsContext = createContext<
  Dispatch<SetStateAction<BreadcrumbItems>>
>(() => undefined);

/** The trail the current page declared; empty on top-level pages. */
export function usePageBreadcrumbItems(): BreadcrumbItems {
  return useContext(BreadcrumbItemsContext);
}

/** Identifies a trail by what is shown and where it leads, so inline arrays do not re-publish every render. */
function toTrailKey(items: BreadcrumbItems): string {
  return items
    .map((item) => [item.label, item.to ?? '', item.onClick ? '1' : ''].join('\u0000'))
    .join('\u0001');
}

/**
 * Declares the current page's position in the hierarchy; the topbar renders it.
 *
 * Call it once per page, unconditionally (before any early return). The trail is
 * cleared when the page unmounts, so top-level pages simply do not call it.
 * Wrappers around other pages (layouts, permission guards) must not call it,
 * because a parent's effect runs after its child's and would overwrite the trail.
 */
export function usePageBreadcrumb(items: BreadcrumbItems) {
  const setItems = useContext(SetBreadcrumbItemsContext);
  const latestItems = useRef(items);
  latestItems.current = items;
  const trailKey = toTrailKey(items);

  useLayoutEffect(() => {
    // onClick handlers are read at click time so a re-created callback never goes stale.
    setItems(
      latestItems.current.map((item, index) =>
        item.onClick ? { ...item, onClick: () => latestItems.current[index]?.onClick?.() } : item,
      ),
    );
  }, [trailKey, setItems]);

  useLayoutEffect(() => () => setItems(EMPTY_BREADCRUMB_ITEMS), [setItems]);
}
