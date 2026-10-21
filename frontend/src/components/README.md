# Components Structure

`src/components` is for shared, reusable UI only. Domain-specific UI for admin, master, or user flows belongs in `src/features`.

## Folders

- `ui`: shadcn/design-system primitives. Keep imports as `@/components/ui/...`. See `src/components/ui/README.md` for token and component customization notes.
- `layout`: app and page layout helpers such as navigation, scroll behavior, form page shells, and table cards.
- `feedback`: dialogs, confirmations, inline help, and user feedback components.
- `data-display`: reusable tables, graph-like display components, pagination, and timelines.
- `forms/shared`: shared form primitives and reusable form helpers.
- `inputs`: reusable input widgets, file selectors, upload buttons, and previous-value fields.
- `address`: address display and Japanese address formatting helpers.

## Boundary Rules

- Do not place admin/master/user-specific components in this folder.
- Keep feature-specific tables and dialogs inside `src/features/*`.
- Prefer direct category imports, for example:
  - `@/components/layout/FormPageWrapper/FormPageWrapper`
  - `@/components/data-display/DataGridTable`
  - `@/components/forms/shared/GenericFormFields/GenericFormFields`
  - `@/components/inputs/FileSelectButton/FileSelectButton`

## Moved Path Reference

- `src/components/FormPageWrapper/*` -> `src/components/layout/FormPageWrapper/*`
- `src/components/ScrollTableCard/*` -> `src/components/layout/ScrollTableCard/*`
- `src/components/Confirmation*` -> `src/components/feedback/*`
- `src/components/ConfirmationData/*` -> `src/components/feedback/ConfirmationData/*`
- `src/components/InlineHelp/*` -> `src/components/feedback/InlineHelp/*`
- `src/components/Pagination/*` -> `src/components/data-display/Pagination/*`
- `src/components/FileSelectButton/*` -> `src/components/inputs/FileSelectButton/*`
- `src/components/forms/GenericFormFields/*` -> `src/components/forms/shared/GenericFormFields/*`
