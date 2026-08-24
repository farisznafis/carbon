import { Wind } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import {
  PARTICIPATION_METHODOLOGIES,
  UNKNOWN_METHODOLOGY,
} from '@/lib/constants/methodology';
import { HOME_SECTION_IDS } from './homeContent';
import { HomeSectionHeading } from './HomeSectionHeading';

const SUPPORTED_METHODOLOGIES = PARTICIPATION_METHODOLOGIES.filter(
  (methodology) => methodology.value !== UNKNOWN_METHODOLOGY,
);

/** Renders only methodologies currently selectable in company registration. */
export function HomeMethodsSection() {
  return (
    <section
      id={HOME_SECTION_IDS.methods}
      aria-labelledby="home-methods-title"
      className="scroll-mt-20 border-y border-border bg-background py-16 sm:py-20 lg:py-24"
    >
      <div className="mx-auto w-full max-w-marketing space-y-10 px-4 sm:px-6 lg:px-8">
        <HomeSectionHeading
          id="home-methods-title"
          title="初期対応方法論"
          description="現在の参加申請で選択できる方法論です。対応範囲は今後順次拡大していきます。"
          align="left"
          className="[&_p]:mx-0"
        />

        <div className="grid max-w-4xl gap-5 md:grid-cols-2">
          {SUPPORTED_METHODOLOGIES.map((methodology) => (
            <Card
              key={methodology.value}
              className="border-border/70 shadow-app-md"
            >
              <CardContent className="flex items-center gap-5 p-6 sm:p-7">
                <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                  <Wind className="h-6 w-6" aria-hidden="true" />
                </span>
                <div className="min-w-0">
                  <p className="text-base font-semibold leading-6 text-primary">
                    {methodology.value}
                  </p>
                  <h3 className="mt-1 text-lg font-semibold leading-7">
                    {methodology.label}
                  </h3>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        <p className="text-sm leading-6 text-muted-foreground">
          ※ 表示は現在の参加企業登録で選択できる方法論です。対応範囲は今後変更される場合があります。
        </p>
      </div>
    </section>
  );
}
