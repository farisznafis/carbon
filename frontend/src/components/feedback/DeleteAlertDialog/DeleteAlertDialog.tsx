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
import { cn } from '@/lib/utils';
import { Trash2 } from 'lucide-react';

type Props = {
    label?: string;          // e.g. "User"
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onDelete: () => void;
    iconOnly?: boolean;
    className?: string;
};

export function DeleteAlertDialog({
    label = 'X',
    open,
    onOpenChange,
    onDelete,
    iconOnly = false,
    className,
}: Readonly<Props>) {
    return (
        <AlertDialog open={open} onOpenChange={onOpenChange}>
            <AlertDialogTrigger asChild>
                <Button
                    type="button"
                    variant="destructive"
                    size={iconOnly ? "icon" : "sm"}
                    className={cn(iconOnly ? "h-10 w-10 rounded-full" : "w-full", className)}
                    aria-label={`${label}を削除`}
                >
                    <Trash2 className="h-4 w-4" />
                    {!iconOnly && ' 削除'}
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
