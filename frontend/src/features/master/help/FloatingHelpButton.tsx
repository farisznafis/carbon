import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, HelpCircle, MessageCircleQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { paths } from '@/routes/paths';
import { GuideModal } from './GuideModal';
import type { GuideContext } from './guideSteps';

type FloatingHelpButtonProps = {
  context: GuideContext;
};

export function FloatingHelpButton({
  context,
}: Readonly<FloatingHelpButtonProps>) {
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
      <div className="fixed bottom-4 right-4 z-40 sm:right-3">
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
            <Button
              type="button"
              variant="ghost"
              onClick={handleOpenGuide}
              className="h-9 w-full justify-start rounded-md px-3"
            >
              <BookOpen className="h-4 w-4 shrink-0 text-muted-foreground" />
              使い方ガイド
            </Button>

            <Button
              type="button"
              variant="ghost"
              onClick={handleOpenFaq}
              className="h-9 w-full justify-start rounded-md px-3"
            >
              <MessageCircleQuestion className="h-4 w-4 shrink-0 text-muted-foreground" />
              ヘルプ・FAQ
            </Button>
          </PopoverContent>
        </Popover>
      </div>

      <GuideModal
        open={guideOpen}
        context={context}
        onClose={() => setGuideOpen(false)}
      />
    </>
  );
}
