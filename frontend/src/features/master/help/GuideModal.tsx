// GuideModal.tsx

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

// ─── Video Player ─────────────────────────────────────────────────────────────

type VideoPlayerProps = {
  src: string;
  poster?: string;
};

function VideoPlayer({ src, poster }: VideoPlayerProps) {
  return (
    <div className="w-full rounded-xl overflow-hidden bg-black aspect-video">
      <video
        className="w-full h-full object-cover"
        src={src}
        poster={poster}
        controls
        playsInline
        preload="metadata"
      />
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

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      onClose();
      setTimeout(() => setCurrentIndex(0), 300);
    }
  };

  const handlePrev = () => setCurrentIndex((prev) => Math.max(prev - 1, 0));
  const handleNext = () => setCurrentIndex((prev) => Math.min(prev + 1, totalSteps - 1));

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="w-full max-w-[580px] p-0 gap-0 overflow-hidden"
        hideCloseButton
      >

        {/* ── ヘッダー ──────────────────────────────────────────────────── */}
        <DialogHeader className="flex flex-row items-center justify-between px-6 pt-6 pb-4 border-b border-border">
          <div className="space-y-0.5">
            <DialogTitle className="text-base font-semibold leading-tight">
              使い方ガイド
            </DialogTitle>
            <p className="text-xs text-muted-foreground tabular-nums">
              ステップ {currentIndex + 1} / {totalSteps}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 opacity-60 hover:opacity-100 hover:bg-muted transition-all focus:outline-none focus:ring-2 focus:ring-ring"
            aria-label="閉じる"
          >
            <X className="h-4 w-4" />
          </button>
        </DialogHeader>

        {/* ── プログレスバー ────────────────────────────────────────────── */}
        <div
          className="w-full h-1 bg-muted"
          role="progressbar"
          aria-valuenow={currentIndex + 1}
          aria-valuemin={1}
          aria-valuemax={totalSteps}
          aria-label="ガイドの進行状況"
        >
          <div
            className="h-full bg-primary transition-all duration-300"
            style={{ width: `${((currentIndex + 1) / totalSteps) * 100}%` }}
          />
        </div>

        {/* ── 本文 ────────────────────────────────────────────────────── */}
        <div className="px-6 py-6 space-y-4">

          {/* ステップラベル・タイトル */}
          <div className="space-y-1.5">
            <p className="text-xs font-medium text-primary uppercase tracking-widest">
              ステップ {currentIndex + 1}
            </p>
            <h3 className="text-lg font-semibold text-foreground leading-snug">
              {step.title}
            </h3>
          </div>

          {/* 説明文 */}
          <p className="text-sm text-muted-foreground leading-relaxed">
            {step.description}
          </p>

          {/* ── 動画 ─────────────────────────────────────────────────── */}
          {step.videoSrc ? (
            <VideoPlayer
              src={step.videoSrc}
              poster={step.videoPoster}
            />
          ) : (
            // 動画がない場合のフォールバック
            <div
              className="flex flex-col items-center justify-center w-full rounded-xl bg-muted text-muted-foreground gap-2"
              style={{ minHeight: '220px' }}
            >
              <div className="h-10 w-10 rounded-full bg-muted-foreground/20 flex items-center justify-center">
                <svg className="h-5 w-5 text-muted-foreground" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z" />
                </svg>
              </div>
              <p className="text-sm">このステップには動画がありません</p>
            </div>
          )}

        </div>

        {/* ── フッター ──────────────────────────────────────────────────── */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-border">

          {/* 左：スキップ（最終ステップでは非表示） */}
          {!isLast ? (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="text-muted-foreground hover:text-foreground"
            >
              スキップ
            </Button>
          ) : (
            <div />
          )}

          {/* 右：戻る / 次へ または 完了 */}
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handlePrev}
              disabled={isFirst}
              className={isFirst ? 'invisible' : ''}
            >
              戻る
            </Button>

            {isLast ? (
              <Button type="button" size="sm" onClick={onClose}>
                完了
              </Button>
            ) : (
              <Button type="button" size="sm" onClick={handleNext}>
                次へ
              </Button>
            )}
          </div>

        </div>
      </DialogContent>
    </Dialog>
  );
}