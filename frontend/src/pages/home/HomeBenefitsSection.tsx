import { useState } from 'react';
import {
  ArrowRight,
  Image as ImageIcon,
} from 'lucide-react';

import {
  HOME_CAPABILITIES,
  HOME_SECTION_IDS,
} from './homeContent';

/**
 * Displays the homepage capabilities as an interactive
 * two-column marketing section.
 *
 * Colors, borders, radius, typography, and elevation use
 * the shared Carbon design-system tokens.
 */
export function HomeBenefitsSection() {
  const [activeIndex, setActiveIndex] = useState(0);

  const activeCapability = HOME_CAPABILITIES[activeIndex];
  const ActiveIcon = activeCapability.icon;

  return (
    <section
      id={HOME_SECTION_IDS.features}
      aria-labelledby="home-features-title"
      className="
        relative
        scroll-mt-20
        overflow-hidden
        surface-brand-dark
        py-16
        sm:py-20
        lg:py-marketing-section
      "
    >
      <div
        className="
          mx-auto
          w-full
          max-w-app
          px-4
          sm:px-6
          lg:px-8
        "
      >
        {/* Section heading */}
        <div
          className="
            grid
            gap-6
            lg:grid-cols-[1.05fr_0.95fr]
            lg:items-end
            lg:gap-[calc(var(--section-gap)*2)]
          "
        >
          <div>
            <h2
              id="home-features-title"
              className="
                text-3xl
                font-bold
                leading-tight
                tracking-tight
                text-background
                sm:text-marketing-title
              "
            >
              このサービスでできること
            </h2>

            <p
              className="
                mt-4
                max-w-xl
                text-lg
                font-medium
                leading-8
                text-background/90
              "
            >
              ひとつの画面ではなく、ひとつの運用としてつなぐ。
            </p>
          </div>

          <p
            className="
              max-w-xl
              text-sm
              leading-7
              text-background/90
              sm:text-base
            "
          >
            {/* 入力・申請・確認・可視化を、それぞれ孤立した機能にしない。Carbon Zeroは業務の前後関係をつなぎ、次の判断につながる状態をつくります。 */}
          </p>
        </div>

        {/* Main capability area */}
        <div
          className="
            mt-8
            grid
            overflow-hidden
            border-y
            border-background/15
            lg:grid-cols-[0.82fr_1.18fr]
          "
        >
          {/* Capability selector */}
          <div
            className="
              border-background/15
              lg:border-r
            "
            aria-label="Carbon Zeroの主要機能"
          >
            {HOME_CAPABILITIES.map((capability, index) => {
              const Icon = capability.icon;
              const isActive = activeIndex === index;

              return (
                <button
                  key={capability.title}
                  type="button"
                  aria-pressed={isActive}
                  onClick={() => setActiveIndex(index)}
                  className={`
                    group
                    grid
                    min-h-32
                    w-full
                    grid-cols-[32px_minmax(0,1fr)_20px]
                    items-center
                    gap-3
                    border-b
                    border-background/10
                    px-4
                    text-left
                    transition-colors
                    last:border-b-0
                    focus-visible:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-inset
                    focus-visible:ring-primary
                    sm:min-h-36
                    sm:grid-cols-[40px_minmax(0,1fr)_24px]
                    sm:gap-4
                    sm:px-6
                    lg:px-0
                    lg:pr-8
                    ${
                      isActive
                        ? 'bg-primary/10'
                        : 'bg-transparent hover:bg-background/5'
                    }
                  `}
                >
                  {/* Number */}
                  <span
                    className={`
                      self-start
                      pt-7
                      font-mono
                      text-xs
                      font-bold
                      ${
                        isActive
                          ? 'text-primary'
                          : 'text-background/40'
                      }
                    `}
                  >
                  </span>

                  {/* Label */}
                  <div className="flex min-w-0 items-center gap-4">
                    <span
                      className={`
                        hidden
                        h-10
                        w-10
                        shrink-0
                        items-center
                        justify-center
                        rounded-lg
                        sm:flex
                        ${
                          isActive
                            ? 'bg-primary text-primary-foreground'
                            : 'bg-background/10 text-background/60'
                        }
                      `}
                    >
                      <Icon
                        className="h-5 w-5"
                        aria-hidden="true"
                      />
                    </span>

                    <div className="min-w-0">
                      <strong
                        className={`
                          block
                          text-base
                          font-bold
                          leading-6
                          ${
                            isActive
                              ? 'text-background'
                              : 'text-background'
                          }
                        `}
                      >
                        {capability.title}
                      </strong>

                      <span
                        className="
                          mt-2
                          block
                          text-sm
                          leading-6
                          text-background
                        "
                      >
                        {capability.summary}
                      </span>
                    </div>
                  </div>

                  {/* Arrow */}
                  <ArrowRight
                    aria-hidden="true"
                    className={`
                      h-4
                      w-4
                      shrink-0
                      transition-transform
                      ${
                        isActive
                          ? 'translate-x-1 text-primary'
                          : 'text-background/30 group-hover:translate-x-1 group-hover:text-background/60'
                      }
                    `}
                  />
                </button>
              );
            })}
          </div>

          {/* Active capability */}
          <div
            className="
              flex
              min-h-[480px]
              flex-col
              px-4
              py-10
              sm:px-8
              lg:min-h-[580px]
              lg:px-12
              lg:py-12
            "
          >
            {/* Active heading */}
            <div className="flex items-start justify-between gap-6">
              <div>
                <h3
                  className="
                    mt-2
                    text-2xl
                    font-bold
                    leading-tight
                    tracking-tight
                    text-background
                    sm:text-form-title
                  "
                >
                  {activeCapability.title}
                </h3>
              </div>
            </div>

            <p
              className="
                mt-4
                max-w-2xl
                text-sm
                leading-7
                text-background
                sm:text-base
              "
            >
              {activeCapability.description}
            </p>

            {/* Screenshot / image area */}
            <div
              key={activeIndex}
              className="
                mt-8
                flex-1
                animate-fade-in
                overflow-hidden
                rounded-lg
                border
                border-border
                bg-card
                shadow-app-md
                motion-reduce:animate-none
              "
            >
              {activeCapability.previewImage ? (
                <img
                  src={activeCapability.previewImage.src}
                  alt={activeCapability.previewImage.alt}
                  loading="lazy"
                  className="
                    block
                    h-auto
                    w-full
                    object-contain
                  "
                />
              ) : (
                <div
                  className="
                    flex
                    h-full
                    min-h-[320px]
                    items-center
                    justify-center
                    bg-gradient-to-br
                    from-muted
                    to-accent
                    p-5
                    sm:min-h-[380px]
                    sm:p-8
                    lg:p-[calc(var(--card-padding)*1.3333)]
                  "
                >
                  <div
                    className="
                      flex
                      h-full
                      min-h-[260px]
                      w-full
                      flex-col
                      items-center
                      justify-center
                      gap-3
                      rounded-md
                      border
                      border-dashed
                      border-border
                      bg-card/70
                      px-6
                      text-center
                      sm:min-h-[320px]
                    "
                  >
                    <span
                      className="
                        flex
                        h-12
                        w-12
                        items-center
                        justify-center
                        rounded-lg
                        bg-accent
                        text-accent-foreground
                      "
                    >
                      <ImageIcon
                        className="h-5 w-5"
                        aria-hidden="true"
                      />
                    </span>

                    <div>
                      <p
                        className="
                          text-sm
                          font-medium
                          text-foreground
                        "
                      >
                        Product screenshot
                      </p>

                      <p
                        className="
                          mt-1
                          text-xs
                          text-muted-foreground
                        "
                      >
                        画面イメージをここに配置
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
