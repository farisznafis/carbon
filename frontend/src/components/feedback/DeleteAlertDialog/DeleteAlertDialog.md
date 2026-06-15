# DeleteAlertDialog Component

A reusable confirmation dialog for destructive delete actions, built on top of the `AlertDialog` and `Button` components (e.g., from shadcn/ui) and `lucide-react`’s `Trash2` icon.

---

## Usage

```tsx
import { useState } from 'react';
import { DeleteAlertDialog } from './DeleteAlertDialog';

function Example() {
  const [open, setOpen] = useState(false);

  const handleDelete = () => {
    // your delete logic
    console.log('Deleted');
  };

  return (
    <DeleteAlertDialog
      label="X"
      open={open}
      onOpenChange={setOpen}
      onDelete={handleDelete}
    />
  );
}
```

---

## Props

```ts
type DeleteAlertDialogProps = {
  /** Label shown in the dialog text: "{label}を削除しますか？" */
  label?: string;          // default: "X"

  /** Controls whether the dialog is open (controlled component). */
  open: boolean;

  /** Called when the dialog open state changes. */
  onOpenChange: (open: boolean) => void;

  /** Called when the user confirms the delete action. */
  onDelete: () => void;
};
```

---

## Behavior

- Renders a **destructive button** with a trash icon and text `削除`.
- When open is `true`, shows:
  - Title: `{label}を削除しますか？`
  - Description: `この操作は取り消せません。{label}を完全に削除します。`
- “キャンセル” closes the dialog via `onOpenChange(false)` (handled by `AlertDialog`).
- “削除する” triggers `onDelete` and uses destructive styling.

---

## Dependencies

- `@/components/ui/alert-dialog`  
- `@/components/ui/button`  
- `lucide-react` (`Trash2` icon)