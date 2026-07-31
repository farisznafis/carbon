// components/ConfirmationModal.tsx
import React, { useMemo } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

export type Change = {
  key: string;
  label?: string;
  before: unknown;
  after: unknown;
};

function computeChanges<T extends Record<string, unknown>>(
  orig: T | undefined,
  next: T | undefined,
): Change[] {
  const changes: Change[] = [];
  const keys = new Set<string>([
    ...Object.keys(orig || {}),
    ...Object.keys(next || {})
  ]);
  keys.forEach((k) => {
    if (k === "emailConfirmation") return; // ignore confirmation field
    const before = orig?.[k];
    const after = next?.[k];
    const beforeNorm = before ?? "";
    const afterNorm = after ?? "";
    if (String(beforeNorm) !== String(afterNorm)) {
      changes.push({ key: k, before, after });
    }
  });
  return changes;
}

interface ConfirmationModalProps<T> {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  originalData?: T;
  nextData?: T;
  labels?: Record<string, string>;
  title?: string;
  description?: string;
}

export default function ConfirmationModal<T extends Record<string, unknown>>({
  isOpen,
  onClose,
  onConfirm,
  originalData,
  nextData,
  labels = {},
  title = "変更内容の確認",
  description = "以下の変更を確認してください。"
}: ConfirmationModalProps<T>) {
  const changes = useMemo(() => computeChanges(originalData, nextData), [originalData, nextData]);

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md w-[92%] rounded-xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          {description && <p className="text-foreground text-sm mt-1">{description}</p>}
        </DialogHeader>

        <div className="mt-4 space-y-3 max-h-[60vh] overflow-y-auto">
          {changes.length === 0 ? (
            <div className="text-sm text-foreground">変更はありません。</div>
          ) : (
            changes.map((c) => (
              <div key={c.key} className="flex gap-4 items-start border-b pb-2">
                <div className="w-1/3 text-sm text-foreground">{labels[c.key] ?? c.key}</div>
                <div className="w-2/3 text-sm">
                  <div className="text-xs text-foreground">Before</div>
                  <div className="font-medium">{String(c.before ?? "—")}</div>
                  <div className="text-xs text-foreground mt-1">After</div>
                  <div className="font-medium">{String(c.after ?? "—")}</div>
                </div>
              </div>
            ))
          )}
        </div>

        <DialogFooter className="mt-4 flex justify-end gap-3">
          <Button variant="outline" onClick={onClose}>戻る</Button>
          <Button onClick={onConfirm} disabled={changes.length === 0}>確定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
