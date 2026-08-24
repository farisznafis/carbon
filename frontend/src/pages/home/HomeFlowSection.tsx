import { Fragment } from 'react';
import { ArrowDown, ArrowRight } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { HOME_FLOW_STEPS, HOME_SECTION_IDS } from './homeContent';
import { HomeSectionHeading } from './HomeSectionHeading';

/** Renders the four-step participation flow with responsive directional cues. */
export function HomeFlowSection() {
  return (
    <section
      id={HOME_SECTION_IDS.flow}
      aria-labelledby="home-flow-title"
      className="relative z-20 scroll-mt-20 rounded-t-3xl bg-card py-16 shadow-marketing sm:py-20 lg:py-24"
    >
      <div className="mx-auto w-full max-w-marketing space-y-10 px-4 sm:px-6 lg:space-y-12 lg:px-8">
        <HomeSectionHeading id="home-flow-title" title="ご利用の流れ" />

        <div className="flex flex-col items-stretch gap-4 xl:flex-row xl:items-center">
          {HOME_FLOW_STEPS.map((step, index) => (
            <Fragment key={step.title}>
              <Card className="flex-1 border-border/70 shadow-app-md">
                <CardContent className="flex h-full flex-col gap-4 p-6">
                  <div className="flex items-center gap-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary font-semibold text-primary">
                      {index + 1}
                    </span>
                    <h3 className="text-lg font-semibold leading-7">
                      {step.title}
                    </h3>
                  </div>
                  <div className="aspect-square overflow-hidden rounded-xl bg-muted/60">
                    <img
                      src={step.imageSrc}
                      alt=""
                      aria-hidden="true"
                      loading="lazy"
                      className="h-full w-full object-contain"
                    />
                  </div>
                  <p className="text-sm leading-6 text-muted-foreground">
                    {step.description}
                  </p>
                </CardContent>
              </Card>

              {index < HOME_FLOW_STEPS.length - 1 ? (
                <div
                  className="flex shrink-0 items-center justify-center text-primary"
                  aria-hidden="true"
                >
                  <ArrowDown className="h-6 w-6 xl:hidden" />
                  <ArrowRight className="hidden h-6 w-6 xl:block" />
                </div>
              ) : null}
            </Fragment>
          ))}
        </div>
      </div>
    </section>
  );
}
