import { useState } from 'react';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { GUIDE_STEPS } from './guideSteps';

// ─── Types ────────────────────────────────────────────────────────────────────

type Props = {
  open: boolean;
  onClose: () => void;
};

// ─── Video placeholder ────────────────────────────────────────────────────────

function VideoPlaceholder() {
  return (
    <div
      className="flex items-center justify-center w-full rounded-lg bg-muted text-muted-foreground text-sm"
      style={{ minHeight: '10rem' }}
      aria-label="動画プレースホルダー"
    >
      操作説明動画がここに表示されます
    </div>
  );
}

// ─── Component ────────────────────────────────────────────────────────────────

export function GuideModal({ open, onClose }: Props) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const totalSteps = GUIDE_STEPS.length;
  const isFirst = currentIndex === 0;
  const isLast = currentIndex === totalSteps - 1;
  const step = GUIDE_STEPS[currentIndex];

  // Reset to step 1 whenever the modal opens
  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      onClose();
      // Delay reset so the closing animation finishes
      setTimeout(() => setCurrentIndex(0), 300);
    }
  };

  const handlePrev = () =>
    setCurrentIndex((prev) => Math.max(prev - 1, 0));

  const handleNext = () =>
    setCurrentIndex((prev) => Math.min(prev + 1, totalSteps - 1));

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        // Cap width at 480px; let it be narrower on small screens
        className="w-full max-w-[480px] p-0 gap-0 overflow-hidden"
        // Hide the default shadcn close icon — we render our own in the footer
        hideCloseButton
      >
        {/* ── Header ──────────────────────────────────────────────────── */}
        <DialogHeader className="flex flex-row items-center justify-between px-5 pt-5 pb-3 border-b border-border">
          <DialogTitle className="text-base font-semibold leading-tight">
            使い方ガイド
          </DialogTitle>
          <button
            type="button"
            onClick={onClose}
            className="rounded-sm opacity-70 hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 transition-opacity"
            aria-label="閉じる"
          >
            <X className="h-4 w-4" />
          </button>
        </DialogHeader>

        {/* ── Body ────────────────────────────────────────────────────── */}
        <div className="px-5 py-5 space-y-4">
          {/* Progress indicator */}
          <p className="text-xs text-muted-foreground text-right tabular-nums">
            {currentIndex + 1} / {totalSteps}
          </p>

          {/* Progress bar */}
          <div
            className="w-full h-1.5 rounded-full bg-muted overflow-hidden"
            role="progressbar"
            aria-valuenow={currentIndex + 1}
            aria-valuemin={1}
            aria-valuemax={totalSteps}
            aria-label="ガイドの進捗"
          >
            <div
              className="h-full rounded-full bg-primary transition-all duration-300"
              style={{
                width: `${((currentIndex + 1) / totalSteps) * 100}%`,
              }}
            />
          </div>

          {/* Step title */}
          <h3 className="text-[15px] font-semibold text-foreground leading-snug">
            {step.title}
          </h3>

          {/* Description */}
          <p className="text-sm text-muted-foreground leading-relaxed">
            {step.description}
          </p>

          {/* Video placeholder */}
          <VideoPlaceholder />
        </div>

        {/* ── Footer ──────────────────────────────────────────────────── */}
        <div className="flex items-center justify-between px-5 pb-5 pt-1 border-t border-border gap-2">
          {/* Left: スキップ */}
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="text-muted-foreground hover:text-foreground"
          >
            スキップ
          </Button>

          {/* Right: 戻る / 次へ or 完了 */}
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handlePrev}
              disabled={isFirst}
              aria-disabled={isFirst}
              className={isFirst ? 'invisible' : ''}
            >
              戻る
            </Button>

            {isLast ? (
              <Button
                type="button"
                size="sm"
                onClick={onClose}
              >
                完了
              </Button>
            ) : (
              <Button
                type="button"
                size="sm"
                onClick={handleNext}
              >
                次へ
              </Button>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}