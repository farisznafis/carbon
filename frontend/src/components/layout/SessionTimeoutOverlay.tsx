import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { FormActions } from '@/components/forms/form-layout';
import { AlertCircle, Clock } from "lucide-react";

interface SessionTimeoutOverlayProps {
  isOpen: boolean;
  onReAuthenticate: () => void;
}

export const SessionTimeoutOverlay = ({ isOpen, onReAuthenticate }: SessionTimeoutOverlayProps) => {
  return (
    <Dialog open={isOpen} modal>
      <DialogContent 
        className="sm:max-w-md" 
        onPointerDownOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        <DialogHeader className="space-y-4">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-warning/15">
            <Clock className="h-6 w-6 text-warning-strong" />
          </div>
          <DialogTitle className="text-center text-xl">
            セッションが期限切れです
          </DialogTitle>
          <DialogDescription className="text-center space-y-3">
            <p className="flex items-center justify-center gap-2 text-foreground">
              <AlertCircle className="h-4 w-4 text-muted-foreground" />
              操作がないため、セッションの有効期限が切れました
            </p>
            <p className="text-sm text-foreground bg-muted/50 p-3 rounded-md">
              ご安心ください。作業内容は保存されています。<br />
              再認証後、続きから作業を再開できます。
            </p>
          </DialogDescription>
        </DialogHeader>
        
        <DialogFooter>
          <FormActions
            context="compact"
            className="w-full"
            actions={[
              {
                id: 'reauthenticate',
                label: 'ログインして続ける',
                onClick: onReAuthenticate,
              },
            ]}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
