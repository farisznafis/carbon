import { LifeBuoy } from 'lucide-react';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { HOME_FAQ_ITEMS, HOME_SECTION_IDS } from './homeContent';
import { HomeSectionHeading } from './HomeSectionHeading';

/** Renders the homepage FAQ and clearly marks unavailable contact navigation. */
export function HomeFaqSection() {
  return (
    <section
      id={HOME_SECTION_IDS.faq}
      aria-labelledby="home-faq-title"
      className="scroll-mt-20 bg-card py-16 sm:py-20 lg:py-24"
    >
      <div className="mx-auto grid w-full max-w-marketing gap-10 px-4 sm:px-6 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:items-center lg:gap-16 lg:px-8">
        <div className="space-y-8">
          <HomeSectionHeading
            id="home-faq-title"
            title="よくあるご質問"
            align="left"
          />
          <Card className="max-w-xl border-border/70 shadow-app-md">
            <CardContent className="space-y-5 p-6">
              <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-primary">
                <LifeBuoy className="h-5 w-5" aria-hidden="true" />
              </span>
              <div className="space-y-2">
                <h3 className="text-lg font-semibold">解決しない場合はこちら</h3>
                <p className="text-sm leading-6 text-muted-foreground">
                  お問い合わせ窓口は現在準備中です。公開後、このページからご案内します。
                </p>
              </div>
              <Button type="button" variant="outline" disabled>
                お問い合わせ（準備中）
              </Button>
            </CardContent>
          </Card>
        </div>

        <Accordion type="single" collapsible defaultValue="multiple-sites">
          {HOME_FAQ_ITEMS.map((faq) => (
            <AccordionItem key={faq.value} value={faq.value}>
              <AccordionTrigger className="py-6 text-left text-base font-semibold leading-7 hover:no-underline sm:text-lg">
                {faq.question}
              </AccordionTrigger>
              <AccordionContent className="pb-6 text-sm leading-6 text-muted-foreground sm:text-base sm:leading-7">
                {faq.answer}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </section>
  );
}
