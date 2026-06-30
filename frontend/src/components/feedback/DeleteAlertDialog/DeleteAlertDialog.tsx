// DeleteAlertDialog.tsx
import {
    AlertDialog,
    AlertDialogTrigger,
    AlertDialogContent,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogCancel,
    AlertDialogAction,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Trash2 } from 'lucide-react';

type Props = {
    label?: string;          // e.g. "User"
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onDelete: () => void;
};

export function DeleteAlertDialog({
    label = 'X',
    open,
    onOpenChange,
    onDelete,
}: Readonly<Props>) {
    return (
        <AlertDialog open={open} onOpenChange={onOpenChange}>
            <AlertDialogTrigger asChild>
                <Button
                    type="button"
                    variant="destructive"
                    size="sm"
                    className="w-full"
                >
                    <Trash2 className="h-4 w-4" /> 削除
                </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{label}を削除しますか？</AlertDialogTitle>
                    <AlertDialogDescription>
                        この操作は取り消せません。{label}を完全に削除します。
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>キャンセル</AlertDialogCancel>
                    <AlertDialogAction
                        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        onClick={onDelete}
                    >
                        削除する
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}

// Usage Example:
/*
<DeleteAlertDialog
  label="X"
  open={isDeleteDialogOpen}
  onOpenChange={setIsDeleteDialogOpen}
  onDelete={onDelete}
/>
*/
