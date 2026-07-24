import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, CircleHelp, HelpCircle, MessageCircleQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { paths } from '@/routes/paths';
import { GuideModal } from './GuideModal';

// ─── Component ────────────────────────────────────────────────────────────────

export function FloatingHelpButton() {
  const navigate = useNavigate();
  const [popoverOpen, setPopoverOpen] = useState(false);
  const [guideOpen, setGuideOpen] = useState(false);

  const handleOpenGuide = () => {
    setPopoverOpen(false);
    setGuideOpen(true);
  };

  const handleOpenFaq = () => {
    setPopoverOpen(false);
    navigate(paths.adminHelpFaq);
  };

  return (
    <>
      {/*
       * Positioned fixed at bottom-right.
       * bottom-20 on mobile  → clears a scroll-to-top button at bottom-4 (h-10)
       * bottom-24 on sm+     → extra breathing room on larger screens
       * Adjust these values to match the exact height of your scroll-to-top button.
       */}
      <div className="fixed bottom-20 right-4 sm:bottom-24 sm:right-6 z-50">
        <Popover open={popoverOpen} onOpenChange={setPopoverOpen}>
          <PopoverTrigger asChild>
            <Button
              type="button"
              size="icon"
              className="h-12 w-12 rounded-full shadow-lg"
              aria-label="ヘルプ・ガイド"
              aria-haspopup="true"
              aria-expanded={popoverOpen}
            >
              <HelpCircle className="h-5 w-5" />
            </Button>
          </PopoverTrigger>

          <PopoverContent
            side="top"
            align="end"
            sideOffset={8}
            className="w-48 p-1.5"
          >
            {/* ── メニュー項目 ──────────────────────────────── */}
            <button
              type="button"
              onClick={handleOpenGuide}
              className="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm text-foreground hover:bg-accent hover:text-accent-foreground transition-colors focus:outline-none focus:bg-accent"
            >
              <BookOpen className="h-4 w-4 shrink-0 text-muted-foreground" />
              使い方ガイド
            </button>

            <button
              type="button"
              onClick={handleOpenFaq}
              className="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm text-foreground hover:bg-accent hover:text-accent-foreground transition-colors focus:outline-none focus:bg-accent"
            >
              <MessageCircleQuestion className="h-4 w-4 shrink-0 text-muted-foreground" />
              ヘルプ・FAQ
            </button>
          </PopoverContent>
        </Popover>
      </div>

      {/* Guide modal is rendered at top level so it overlays everything */}
      <GuideModal open={guideOpen} onClose={() => setGuideOpen(false)} />
    </>
  );
}