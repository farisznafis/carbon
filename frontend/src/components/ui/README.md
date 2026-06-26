# UI Design System Primitives

This folder contains shared design-system primitives used across the app.
When you need to change global colors, shape, spacing, typography, or component
states, start with the files listed below.

## Source of Truth

| Area | File | Purpose | How to Change |
| --- | --- | --- | --- |
| Design tokens | `src/index.css` | Brand colors, background, text, border, radius, shadow, and global typography | Update CSS variables in `:root`. Color values should be written as HSL. |
| Tailwind token names | `tailwind.config.ts` | Exposes tokens as Tailwind classes, such as `bg-disabled` or `shadow-app-sm` | Add mappings in `theme.extend`. Prefer tokens over hardcoded component colors. |
| Button | `src/components/ui/button.tsx` | Primary, destructive, subtle, ghost, cancel, and icon buttons | Update `buttonVariants`. If you add a variant or size, add it to the story too. |
| Text input | `src/components/ui/input.tsx` | Single-line form input | Update the base class. Error state is driven by `aria-invalid`. |
| Textarea | `src/components/ui/textarea.tsx` | Multiline form input | Update the base class. Keep tokens aligned with `Input`. |
| Select | `src/components/ui/select.tsx` | Form dropdown/select input | Update `SelectTrigger`, `SelectContent`, and `SelectItem`. |
| File input | `src/components/ui/file-input.tsx` | Reusable file picker/dropzone | Update label layout, icon, helper text, and selected-file behavior here. |
| Required label | `src/components/forms/shared/RequiredLabel.tsx` | Form label with a required badge | Update badge styles in `RequiredBadge`. The badge color uses the `destructive` token. |

## Tokens

The main tokens live in `src/index.css`.

```css
:root {
  --primary: 117.2 17.3% 48.8%;
  --secondary: 66.7 58.1% 87.8%;
  --foreground: 193.3 12.7% 13.9%;
  --background: 0 0% 96.5%;
  --destructive: 0 73.7% 40.2%;
}
```

How to change a color:

1. Convert the hex value to HSL.
2. Update the variable in `:root`.
3. If the new token must be usable as a Tailwind class, add it to `tailwind.config.ts`.

Example:

```css
--primary: 117.2 17.3% 48.8%;
```

Then use it in a component:

```tsx
<div className="bg-primary text-primary-foreground" />
```

## Button

Use `Button` for all standard clickable actions.

```tsx
import { Button } from '@/components/ui/button';

<Button>Continue</Button>
<Button variant="destructive">Delete</Button>
<Button variant="subtle">Cancel</Button>
<Button size="icon" aria-label="Add">
  <Plus />
</Button>
```

Available variants:

| Variant | Use Case |
| --- | --- |
| `default` / `primary` | Main page or form action |
| `destructive` | Dangerous actions such as delete or irreversible cancel |
| `secondary` | Supporting action that still needs visual weight |
| `outline` / `subtle` | Neutral action or secondary choice |
| `cancel` | Back/cancel action that should feel lightweight |
| `ghost` | Low-emphasis action without a border |
| `link` | Text-link style action |

How to change it:

- Update variant styles in `buttonVariants`.
- Update height and padding in the `size` variants.
- Add or update examples in `button.stories.tsx` so the change is visible in Storybook.

## Input, Textarea, Select

Use these form primitives to keep sizing, borders, placeholders, focus rings,
disabled states, and error states consistent.

```tsx
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

<Input placeholder="Email" />
<Input aria-invalid placeholder="Email" />
<Textarea placeholder="Enter details" />
```

Error state:

- `Input` and `Textarea` read `aria-invalid`.
- Forms using `react-hook-form` can show the message through `FormMessage`.

How to change them:

- Update the base class in `input.tsx`, `textarea.tsx`, or `select.tsx`.
- Avoid hardcoded colors such as `text-gray-500`; use tokens such as `text-muted-foreground`.
- If you add a new state, document it in `input.stories.tsx`.

## FileInput

`FileInput` is a controlled single-file picker/dropzone.
Older components such as `FileSelectButton` still exist so migrations can happen
gradually.

```tsx
import { useState } from 'react';
import { FileInput } from '@/components/ui/file-input';

function Example() {
  const [file, setFile] = useState<File | null>(null);

  return (
    <FileInput
      accept=".pdf,.jpg,.png"
      helperText="PDF, JPG, PNG only"
      label="Select file"
      value={file}
      onFileChange={setFile}
    />
  );
}
```

Important props:

| Prop | Purpose |
| --- | --- |
| `value` | The selected file |
| `onFileChange` | Callback fired when a file is selected or cleared |
| `label` | Text shown when no file is selected |
| `helperText` | Helper copy shown below the field |
| `error` | Error message and destructive styling |
| `accept` | Accepted file types |
| `disabled` | Disables the field |

How to change it:

- Update visual layout and states in `file-input.tsx`.
- Add new states to `file-input.stories.tsx`.
- Do not modify the legacy `FileSelectButton` unless you are intentionally migrating old usage.

## RequiredLabel

Use `RequiredLabel` for required fields.

```tsx
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';

<RequiredLabel>Email address</RequiredLabel>
<RequiredLabel required={false}>Fax number</RequiredLabel>
```

How to change it:

- The badge is rendered by `RequiredBadge`.
- The badge color uses the `destructive` token.
- Override the default badge text through the `requiredText` prop when needed.

## Storybook

Storybook is the visual documentation for these components. `*.stories.tsx`
files do not affect the runtime app.

Run:

```bash
npm run storybook
```

Relevant stories:

| Story | Contents |
| --- | --- |
| `button.stories.tsx` | Variants, sizes, icons, disabled states |
| `input.stories.tsx` | Input, textarea, error, disabled states |
| `file-input.stories.tsx` | Empty, error, disabled states |

When changing a component, update the matching story so reviewers can inspect
the visual change without opening many app pages.

## Change Checklist

Before committing design-system changes:

1. Update global tokens first if the change is about colors, radius, shadow, or typography.
2. Update the primitive component if the change is about behavior, state, variant, or size.
3. Update Storybook stories for affected states.
4. Check at least one real page as a pilot, for example the signup form.
5. Run:

```bash
npx eslint src/components
npm run build
```

