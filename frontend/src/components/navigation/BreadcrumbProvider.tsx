import { useState, type ReactNode } from 'react';

import {
  BreadcrumbItemsContext,
  EMPTY_BREADCRUMB_ITEMS,
  SetBreadcrumbItemsContext,
  type BreadcrumbItems,
} from './pageBreadcrumbContext';

/** Holds the trail declared by the current page so the app shell can render it in the topbar. */
export function BreadcrumbProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [items, setItems] = useState<BreadcrumbItems>(EMPTY_BREADCRUMB_ITEMS);

  return (
    <SetBreadcrumbItemsContext.Provider value={setItems}>
      <BreadcrumbItemsContext.Provider value={items}>{children}</BreadcrumbItemsContext.Provider>
    </SetBreadcrumbItemsContext.Provider>
  );
}
