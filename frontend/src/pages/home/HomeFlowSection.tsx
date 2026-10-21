import { HOME_FLOW_STEPS, HOME_SECTION_IDS } from './homeContent';

/**
 * Displays the Carbon Zero business process as one continuous flow.
 *
 * Desktop:
 * Horizontal seven-step timeline.
 *
 * Mobile:
 * Keeps the same horizontal relationship and allows scrolling
 * instead of turning the seven steps into a very long vertical list.
 *
 * All visual styles use shared Carbon design-system tokens.
 */
export function HomeFlowSection() {
  return (
    <section
      id={HOME_SECTION_IDS.flow}
      aria-labelledby='home-flow-title'
      className='
        relative
        scroll-mt-20
        overflow-hidden
        bg-card
        py-16
        sm:py-20
        lg:py-marketing-section
      '
    >
      <div
        className='
          mx-auto
          w-full
          max-w-page
          px-4
          sm:px-6
          lg:px-8
        '
      >
        {/* =====================================================
            SECTION HEADER
            ===================================================== */}
        <div
          className='
            grid
            gap-6
            lg:grid-cols-[1.05fr_0.95fr]
            lg:items-end
            lg:gap-[calc(var(--section-gap)*2)]
          '
        >
          <div>
            <h2
              id='home-flow-title'
              className='
                text-3xl
                font-bold
                leading-tight
                tracking-tight
                text-foreground
                sm:text-marketing-title
              '
            >
              ご利用の流れ
            </h2>

            <p
              className='
                mt-4
                max-w-xl
                text-lg
                font-medium
                leading-8
                text-foreground
              '
            >
              登録から削減量の可視化まで。一本の運用線でつなぐ。
            </p>
          </div>
        </div>

        {/* =====================================================
            BUSINESS FLOW
            ===================================================== */}
        <div className='mt-12 lg:mt-[calc(var(--section-gap)*2)]'>
          {/* Mobile scroll hint */}
          <div
            className='
              mb-4
              flex
              items-center
              justify-between
              sm:hidden
            '
          >
            <span className='text-xs text-muted-foreground'>
              横にスクロールして確認
            </span>

            <span
              aria-hidden='true'
              className='text-sm text-primary-strong'
            >
              →
            </span>
          </div>

          {/* Horizontal scroll container */}
          <div
            className='
              -mx-4
              overflow-x-auto
              px-4
              pb-4
              sm:-mx-6
              sm:px-6
              lg:mx-0
              lg:overflow-visible
              lg:px-0
              lg:pb-0
            '
          >
            <div
              className='
                relative
                min-w-[960px]
                lg:min-w-0
              '
            >
              {/* Main connecting line */}
              <div
                aria-hidden='true'
                className='
                  absolute
                  left-[7.14%]
                  right-[7.14%]
                  top-8
                  h-px
                  bg-border
                '
              />

              <ol
                className='
                  relative
                  z-10
                  grid
                  grid-cols-7
                '
              >
                {HOME_FLOW_STEPS.map((step, index) => {
                  const number = String(index + 1);

                  return (
                    <li
                      key={step.title}
                      className="
                        group
                        relative
                        px-3
                        first:pl-0
                        last:pr-0
                      "
                    >
                      {/* Connector to next step */}
                      {index < HOME_FLOW_STEPS.length - 1 ? (
                        <span
                          aria-hidden="true"
                          className="
                            absolute
                            left-1/2
                            top-8
                            h-px
                            w-full
                            bg-border
                          "
                        />
                      ) : null}

                      {/* Number node */}
                      <div
                        className="
                          relative
                          z-10
                          flex
                          h-16
                          items-center
                          justify-center
                        "
                      >
                        <span
                          className="
                            relative
                            flex
                            h-12
                            w-12
                            items-center
                            justify-center
                            rounded-full
                            border
                            border-border
                            bg-card
                            text-xs
                            font-bold
                            text-primary-strong
                            shadow-app-sm
                            transition
                            duration-200
                            group-hover:border-primary/50
                            group-hover:bg-accent
                          "
                        >
                          {number}
                        </span>
                      </div>

                      {/* Step content */}
                      <div className="mt-8 pt-5">
                        <h3
                          className="
                            min-h-12
                            text-base
                            font-bold
                            leading-6
                            text-foreground
                          "
                        >
                          {step.title}
                        </h3>

                        <p
                          className="
                            
                            text-sm
                            leading-6
                            text-muted-foreground
                          "
                        >
                          {step.description}
                        </p>
                      </div>
                    </li>
                  );
                })}
              </ol>
            </div>
          </div>
        </div>

        {/* =====================================================
            FLOW SUMMARY
            ===================================================== */}
        {/* <div
          className='
            mt-10
            flex
            flex-col
            gap-4
            border-t
            border-border
            pt-6
            sm:flex-row
            sm:items-center
            sm:justify-between
          '
        >
          <p
            className='
              text-sm
              font-medium
              text-foreground
            '
          >
            登録するだけで終わらない。
            <span className='text-primary-strong'> 次の判断までつながる運用へ。</span>
          </p>
        </div> */}
      </div>
    </section>
  );
}
