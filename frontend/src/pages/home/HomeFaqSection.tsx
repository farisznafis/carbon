import { LifeBuoy } from 'lucide-react';
import { Link } from 'react-router-dom';
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

type HomeFaqSectionProps = Readonly<{
  contactPath: string;
}>;

/** Renders the homepage FAQ and its contact-page entry point. */
export function HomeFaqSection({ contactPath }: HomeFaqSectionProps) {
  return (
    <section
      id={HOME_SECTION_IDS.faq}
      aria-labelledby="home-faq-title"
      className="scroll-mt-20 bg-card py-14 sm:py-16 2xl:py-marketing-section"
    >
      <div className="mx-auto grid w-full max-w-page gap-8 px-4 sm:gap-10 sm:px-6 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:items-center lg:gap-[calc(var(--section-gap)*1.25)] lg:px-8 2xl:gap-[calc(var(--section-gap)*2)]">
        <div className="space-y-8">
          <HomeSectionHeading
            id="home-faq-title"
            title="よくあるご質問"
            align="left"
          />
          <Card className="max-w-xl border-border/70 shadow-app-md">
            <CardContent className="space-y-5 p-6 lg:p-[var(--card-padding)]">
              {/* <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-primary">
                <LifeBuoy className="h-5 w-5" aria-hidden="true" />
              </span> */}
              <div className="space-y-2">
                <h3 className="text-lg font-bold">解決しない場合はこちら</h3>
                <p className="text-sm leading-6 text-muted-foreground">
                  サービスに関するご質問や導入のご相談は、お問い合わせフォームからご連絡ください。
                </p>
              </div>
              <Button asChild variant="outline">
                <Link to={contactPath}>お問い合わせ</Link>
              </Button>
            </CardContent>
          </Card>
        </div>

        <Accordion type="single" collapsible defaultValue="multiple-sites">
          {HOME_FAQ_ITEMS.map((faq) => (
            <AccordionItem key={faq.value} value={faq.value}>
              <AccordionTrigger className="py-5 text-left text-base font-bold leading-7 hover:no-underline sm:text-lg 2xl:py-6">
                {faq.question}
              </AccordionTrigger>
              <AccordionContent className="pb-5 text-sm leading-6 text-neutral-800 sm:text-base sm:leading-7 2xl:pb-6">
                {faq.answer}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </section>
  );
}
