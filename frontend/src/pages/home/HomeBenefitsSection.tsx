import type { LucideIcon } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import {
  HOME_CAPABILITIES,
  HOME_HIGHLIGHTS,
  HOME_SECTION_IDS,
  type HomeHighlight,
} from './homeContent';
import { HomeSectionHeading } from './HomeSectionHeading';

type CapabilityCardProps = Readonly<{
  icon: LucideIcon;
  title: string;
  description: string;
}>;

type HighlightCardProps = Readonly<{
  highlight: HomeHighlight;
  className?: string;
}>;

/** Renders one compact capability card from the homepage content model. */
function CapabilityCard({
  icon: Icon,
  title,
  description,
}: CapabilityCardProps) {
  return (
    <Card className="h-full border-border/70 shadow-app-md">
      <CardContent className="flex h-full min-h-56 flex-col justify-center gap-4 p-6 sm:p-8">
        <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-primary">
          <Icon className="h-6 w-6" aria-hidden="true" />
        </span>
        <div className="space-y-2">
          <h3 className="text-xl font-semibold leading-7">{title}</h3>
          <p className="text-sm leading-6 text-muted-foreground">
            {description}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

/** Renders a large bento highlight with an optional dashboard preview. */
function HighlightCard({ highlight, className }: HighlightCardProps) {
  const Icon = highlight.icon;
  const isPrimary = highlight.tone === 'primary';

  return (
    <Card
      className={cn(
        'overflow-hidden border-border/70 shadow-app-md',
        isPrimary && 'border-primary bg-primary text-primary-foreground',
        className,
      )}
    >
      <CardContent
        className={cn(
          'flex h-full min-h-72 flex-col justify-center gap-5 p-7 sm:p-9',
          highlight.media && 'justify-between',
        )}
      >
        <div className="space-y-4">
          <span
            className={cn(
              'flex h-12 w-12 items-center justify-center rounded-xl',
              isPrimary
                ? 'bg-card text-primary'
                : 'bg-secondary text-primary',
            )}
          >
            <Icon className="h-6 w-6" aria-hidden="true" />
          </span>
          <div className="space-y-2">
            <h3 className="text-xl font-semibold leading-7 sm:text-2xl">
              {highlight.title}
            </h3>
            <p
              className={cn(
                'text-sm leading-6 sm:text-base',
                isPrimary
                  ? 'text-primary-foreground/90'
                  : 'text-muted-foreground',
              )}
            >
              {highlight.description}
            </p>
          </div>
        </div>

        {highlight.media === 'dashboard' ? (
          <div className="aspect-[728/210] overflow-hidden rounded-xl border border-primary-foreground/30 bg-card">
            <img
              src="/assets/hero/dashboard-preview.png"
              alt=""
              aria-hidden="true"
              loading="lazy"
              className="h-full w-full object-cover object-top"
            />
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

/** Renders the overlapping Figma benefits surface and its responsive bento grid. */
export function HomeBenefitsSection() {
  return (
    <section
      id={HOME_SECTION_IDS.features}
      aria-labelledby="home-features-title"
      className="relative z-10 -mt-10 scroll-mt-20 rounded-t-3xl bg-card py-16 shadow-marketing sm:-mt-8 sm:py-20 lg:mt-0 lg:py-24"
    >
      <div className="mx-auto w-full max-w-marketing space-y-10 px-4 sm:px-6 lg:space-y-12 lg:px-8">
        <HomeSectionHeading
          id="home-features-title"
          title="このサービスでできること"
        />

        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
          {HOME_CAPABILITIES.map((capability) => (
            <CapabilityCard key={capability.title} {...capability} />
          ))}
        </div>

        <div className="grid gap-5 lg:grid-cols-12">
          <HighlightCard
            highlight={HOME_HIGHLIGHTS[0]}
            className="lg:col-span-8"
          />
          <HighlightCard
            highlight={HOME_HIGHLIGHTS[1]}
            className="lg:col-span-4"
          />
          <HighlightCard
            highlight={HOME_HIGHLIGHTS[2]}
            className="lg:col-span-5"
          />
          <HighlightCard
            highlight={HOME_HIGHLIGHTS[3]}
            className="lg:col-span-7"
          />
        </div>
      </div>
    </section>
  );
}
