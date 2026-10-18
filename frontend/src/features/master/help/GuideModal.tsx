import { useEffect, useState } from 'react';
import { Play, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import {
  GUIDE_CONTENT,
  type GuideContext,
} from './guideSteps';

type GuideModalProps = {
  open: boolean;
  context: GuideContext;
  onClose: () => void;
};

type GuideMediaProps = {
  src: string;
  poster?: string;
  title: string;
};

function GuideMedia({ src, poster, title }: Readonly<GuideMediaProps>) {
  const isImage = /\.(gif|png|jpe?g|webp|avif)(?:\?.*)?$/i.test(src);

  if (isImage) {
    return (
      <div className="aspect-video w-full overflow-hidden rounded-lg bg-muted">
        <img
          src={src}
          alt={title}
          className="h-full w-full object-cover"
        />
      </div>
    );
  }

  return (
    <div className="aspect-video w-full overflow-hidden rounded-lg bg-black">
      <video
        className="h-full w-full object-cover"
        src={src}
        poster={poster}
        controls
        playsInline
        preload="metadata"
      />
    </div>
  );
}

export function GuideModal({
  open,
  context,
  onClose,
}: Readonly<GuideModalProps>) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const content = GUIDE_CONTENT[context];
  const totalSteps = content.steps.length;
  const currentStep = content.steps[currentIndex];
  const isFirst = currentIndex === 0;
  const isLast = currentIndex === totalSteps - 1;
  const progress = ((currentIndex + 1) / totalSteps) * 100;

  useEffect(() => {
    setCurrentIndex(0);
  }, [context, open]);

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) onClose();
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="w-[calc(100vw-2rem)] max-w-xl gap-0 overflow-hidden rounded-lg p-0"
        hideCloseButton
      >
        <DialogHeader className="border-b border-border px-6 py-5 text-left">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1">
              <DialogTitle>{content.title}</DialogTitle>
              <DialogDescription>{content.description}</DialogDescription>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="-mr-2 -mt-2 shrink-0"
              aria-label="閉じる"
              onClick={onClose}
            >
              <X />
            </Button>
          </div>
          <div className="space-y-2 pt-3">
            <div className="flex items-center justify-between text-xs text-foreground">
              <span>
                ステップ {currentIndex + 1} / {totalSteps}
              </span>
              <span>{Math.round(progress)}%</span>
            </div>
            <Progress
              value={progress}
              className="h-2"
              aria-label="ガイドの進行状況"
            />
          </div>
        </DialogHeader>

        <div className="space-y-5 px-6 py-6">
          <div className="space-y-2">
            <p className="text-xs font-medium text-primary">
              STEP {currentIndex + 1}
            </p>
            <h3 className="text-lg font-bold text-foreground">
              {currentStep.title}
            </h3>
            <p className="text-sm leading-6 text-foreground">
              {currentStep.description}
            </p>
          </div>

          {currentStep.mediaSrc ? (
            <GuideMedia
              src={currentStep.mediaSrc}
              poster={currentStep.mediaPoster}
              title={currentStep.title}
            />
          ) : (
            <div className="flex min-h-52 w-full flex-col items-center justify-center gap-2 rounded-lg bg-muted text-foreground">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-muted-foreground/20">
                <Play className="h-5 w-5 fill-current text-muted-foreground" />
              </span>
              <p className="text-sm">このステップには動画がありません</p>
            </div>
          )}
        </div>

        <div className="flex items-center justify-between border-t border-border px-6 py-4">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onClose}
          >
            スキップ
          </Button>

          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={isFirst}
              onClick={() =>
                setCurrentIndex((previous) => Math.max(previous - 1, 0))
              }
            >
              戻る
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={() => {
                if (isLast) {
                  onClose();
                  return;
                }
                setCurrentIndex((previous) =>
                  Math.min(previous + 1, totalSteps - 1),
                );
              }}
            >
              {isLast ? '完了' : '次へ'}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
